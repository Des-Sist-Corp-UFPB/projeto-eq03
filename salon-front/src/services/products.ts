import api from './api';

export interface ProductData {
  id?: number;
  name: string;
  stock: number;
  price: number;
}

export const productsApi = {
  findAll: async () => {
    const { data } = await api.get<ProductData[]>('/products');
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
  }
};
