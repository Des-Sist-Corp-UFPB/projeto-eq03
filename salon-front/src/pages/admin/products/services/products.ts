import api from '../../../../services/api';

export interface ProductData {
  id?: number;
  name: string;
  stock: number;
  price: number;
  active?: boolean;
}

export interface ProductFilter {
  name?: string;
  active?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}

export const productsApi = {
  findAll: async (filter: ProductFilter = {}, page = 0, size = 10) => {
    const { data } = await api.get<PageResponse<ProductData>>('/products', {
      params: { ...filter, page, size },
    });
    return data;
  },

  findById: async (id: number) => {
    const { data } = await api.get<ProductData>(`/products/${id}`);
    return data;
  },

  create: async (productData: ProductData) => {
    const { data } = await api.post<ProductData>('/products', productData);
    return data;
  },

  update: async (id: number, productData: ProductData) => {
    const { data } = await api.put<ProductData>(`/products/${id}`, productData);
    return data;
  },

  delete: async (id: number) => {
    await api.delete(`/products/${id}`);
  },

  reactivate: async (id: number) => {
    const { data } = await api.patch<ProductData>(`/products/${id}/reactivate`);
    return data;
  },
};

