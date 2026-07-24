// Spring Boot 4 aninha os metadados de paginação sob "page" em vez de
// devolvê-los soltos no corpo da resposta. Este helper normaliza isso para o
// formato flat que o resto do frontend (Table, DataTable) já espera.
export interface SpringPageResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export function normalizePage<T>(raw: SpringPageResponse<T>): PageResponse<T> {
  return {
    content: raw.content,
    totalPages: raw.page.totalPages,
    totalElements: raw.page.totalElements,
    size: raw.page.size,
    number: raw.page.number,
  };
}
