export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export interface JwtPayload {
  sub: string;
  role: string;
  userId: number;
  exp: number;
  iat: number;
}

export interface UserContextData {
  email: string;
  role: string;
  userId: number;
  permissions: string[];
  cpf?: string | null;
}

/**
 * DTO retornado pelo endpoint GET /v1/auth/me.
 * Contém o perfil completo do usuário com permissões do banco de dados.
 */
export interface UserProfileResponse {
  userId: number;
  email: string;
  name: string;
  role: string;
  cpf: string | null;
  permissions: string[];
}
