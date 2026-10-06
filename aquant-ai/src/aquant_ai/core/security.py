import base64
import ipaddress
import os
from dataclasses import dataclass
from typing import Annotated
from urllib.parse import urlsplit

import jwt
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from aquant_ai.core.config import Settings, get_settings

bearer_scheme = HTTPBearer(auto_error=False)


@dataclass(frozen=True)
class CurrentUser:
    """当前用户。"""

    user_id: int
    username: str | None


class CredentialCipher:
    """使用 AES-256-GCM 加解密用户的模型 API Key。"""

    def __init__(self, plaintext_key: str | None):
        if not plaintext_key:
            raise RuntimeError("未配置 MODEL_CREDENTIAL_MASTER_KEY")
        key = plaintext_key.encode("utf-8")
        if len(key) != 32:
            raise RuntimeError("MODEL_CREDENTIAL_MASTER_KEY 按 UTF-8 编码后必须为 32 字节")
        self._aes = AESGCM(key)

    def encrypt(self, plaintext: str) -> str:
        """使用随机 nonce 加密明文，返回可直接入库的 Base64 文本。"""
        nonce = os.urandom(12)
        ciphertext = self._aes.encrypt(nonce, plaintext.encode("utf-8"), None)
        return base64.urlsafe_b64encode(nonce + ciphertext).decode("ascii")

    def decrypt(self, encrypted: str) -> str:
        """解密数据库中的 API Key 密文。"""
        payload = base64.urlsafe_b64decode(encrypted.encode("ascii"))
        if len(payload) <= 12:
            raise RuntimeError("模型密钥密文格式无效")
        return self._aes.decrypt(payload[:12], payload[12:], None).decode("utf-8")


def validate_public_https_url(value: str) -> str:
    """校验用户填写的模型地址，提前拦截明显的 SSRF 目标。"""
    normalized = value.strip().rstrip("/")
    parsed = urlsplit(normalized)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password:
        raise ValueError("模型服务地址必须是无用户信息的 HTTPS 地址")
    hostname = parsed.hostname.lower()
    if hostname == "localhost" or hostname.endswith(".localhost"):
        raise ValueError("模型服务地址不能指向本机")
    try:
        address = ipaddress.ip_address(hostname)
    except ValueError:
        return normalized
    if not address.is_global:
        raise ValueError("模型服务地址不能指向内网或保留地址")
    return normalized


BearerCredentials = Annotated[
    HTTPAuthorizationCredentials | None,
    Depends(bearer_scheme),
]
SettingsDep = Annotated[Settings, Depends(get_settings)]


def get_current_user(credentials: BearerCredentials, settings: SettingsDep) -> CurrentUser:
    """解析签发的 JWT，生成用户身份。"""
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="请先登录")
    if not settings.aquant_jwt_secret:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="服务未配置 JWT 密钥",
        )
    try:
        payload = jwt.decode(
            credentials.credentials,
            settings.aquant_jwt_secret,
            algorithms=["HS256"],
            issuer="aquant",
        )
        user_id = int(payload["userId"])
    except (jwt.PyJWTError, KeyError, TypeError, ValueError) as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="登录状态无效或已过期",
        ) from exc
    return CurrentUser(user_id=user_id, username=payload.get("username"))
