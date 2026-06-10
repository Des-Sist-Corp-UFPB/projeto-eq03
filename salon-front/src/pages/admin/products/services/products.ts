import api from '../../../../services/api';

export interface ProductData {
  id?: number;
  name: string;
  stock: number;
  price: number;
  active?: boolean;
}

export const productsApi = {
  findAll: async (active?: boolean) => {
    const params = active !== undefined ? { active } : {};
    const { data } = await api.get<ProductData[]>('/products', { params });
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
};
