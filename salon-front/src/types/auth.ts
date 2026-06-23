export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export interface JwtPayload {
  sub: string;
  role: string;
  userId: number;
  authorities: string[];
  exp: number;
  iat: number;
}

export interface UserContextData {
  email: string;
  role: string;
  userId: number;
  authorities: string[];
  cpf?: string | null;
}
