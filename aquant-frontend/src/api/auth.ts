import request from '@/utils/request';

export interface LoginReq {
    username: string;
    password: string;
}

export interface RegisterReq {
    username: string;
    password: string;
    nickname?: string;
    email?: string;
}

export interface LoginResp {
    token: string;
    nickname: string;
    username: string;
}

export interface UserInfo {
    id: number;
    username: string;
    nickname: string;
    email: string;
}

export interface UpdateEmailReq {
    email: string;
}

export interface SendResetCodeReq {
    email: string;
}

export interface ResetPasswordReq {
    email: string;
    code: string;
    newPassword: string;
}

export function login(data: LoginReq) {
    return request.post<any>('/auth/login', data);
}

export function register(data: RegisterReq) {
    return request.post<any>('/auth/register', data);
}

export function getUserInfo() {
    return request.get<any>('/auth/userInfo');
}

export function updateEmail(data: UpdateEmailReq) {
    return request.post<any>('/auth/updateEmail', data);
}

export function sendResetCode(data: SendResetCodeReq) {
    return request.post<any>('/auth/password/sendResetCode', data);
}

export function resetPassword(data: ResetPasswordReq) {
    return request.post<any>('/auth/password/reset', data);
}
