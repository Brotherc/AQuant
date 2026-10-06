import pytest

from aquant_ai.core.security import CredentialCipher, validate_public_https_url


def test_credential_cipher_round_trip() -> None:
    cipher = CredentialCipher("AQuant@2025!ModelCredentialKey#1")

    encrypted = cipher.encrypt("sk-test-secret")

    assert encrypted != "sk-test-secret"
    assert cipher.decrypt(encrypted) == "sk-test-secret"


@pytest.mark.parametrize(
    "url",
    [
        "http://api.example.com/v1",
        "https://localhost/v1",
        "https://127.0.0.1/v1",
        "https://10.0.0.8/v1",
        "https://user:password@api.example.com/v1",
    ],
)
def test_validate_public_https_url_rejects_unsafe_urls(url: str) -> None:
    with pytest.raises(ValueError):
        validate_public_https_url(url)


def test_validate_public_https_url_normalizes_trailing_slash() -> None:
    assert validate_public_https_url(" https://api.example.com/v1/ ") == (
        "https://api.example.com/v1"
    )
