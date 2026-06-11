import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { productsApi } from './services/products';
import type { ProductData } from './services/products';
import { getApiErrorMessage } from '../../../utils/apiError';
import { useAlert } from '../../../hooks/useAlert';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export const Products = () => {
  const [products, setProducts] = useState<ProductData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filterActive, setFilterActive] = useState<boolean | undefined>(undefined);

  const [showForm, setShowForm] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ProductData | null>(null);

  const [showConfirm, setShowConfirm] = useState(false);
  const [productToDelete, setProductToDelete] = useState<number | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<ProductData>();
  const { error: showError } = useAlert();

  const loadProducts = async () => {
    setIsLoading(true);
    try {
      const data = await productsApi.findAll(filterActive);
      setProducts(data);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao carregar produtos');
      await showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, [filterActive]);

  const handleOpenForm = (product?: ProductData) => {
    reset();
    if (product) {
      setEditingProduct(product);
      setValue('name', product.name);
      setValue('stock', product.stock);
      setValue('price', product.price);
      setValue('active', product.active !== false);
    } else {
      setEditingProduct(null);
      setValue('stock', 0);
      setValue('active', true);
    }
    setShowForm(true);
  };

  const onSubmit = async (data: ProductData) => {
    try {
      if (editingProduct?.id) {
        await productsApi.update(editingProduct.id, data);
      } else {
        await productsApi.create(data);
      }
      setShowForm(false);
      loadProducts();
    } catch (err) {
      const msg = getApiErrorMessage(
        err,
        'Erro ao salvar produto. Verifique os dados e tente novamente.'
      );
      await showError(msg);
    }
  };

  const confirmDelete = async () => {
    if (!productToDelete) return;
    try {
      await productsApi.delete(productToDelete);
      setShowConfirm(false);
      loadProducts();
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao excluir produto.');
      await showError(msg);
    }
  };

  const columns = [
    { key: 'name', label: 'Nome do Produto' },
    { key: 'price', label: 'Preço', render: (item: ProductData) => `R$ ${item.price.toFixed(2)}` },
    { key: 'stock', label: 'Estoque' },
    {
      key: 'active',
      label: 'Status',
      render: (item: ProductData) => (item.active !== false ? 'Ativo' : 'Inativo'),
    },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: ProductData) => (
        <div className="flex gap-2">
          <PermissionGate method="PUT" endpoint={`/v1/products/${item.id}`}>
            <button
              onClick={() => handleOpenForm(item)}
              className="p-1.5 text-indigo-600 hover:bg-indigo-50 border border-indigo-200 rounded-lg transition-all cursor-pointer"
            >
              <Edit size={15} />
            </button>
          </PermissionGate>
          <PermissionGate method="DELETE" endpoint={`/v1/products/${item.id}`}>
            <button
              onClick={() => {
                setProductToDelete(item.id!);
                setShowConfirm(true);
              }}
              className="p-1.5 text-rose-600 hover:bg-rose-50 border border-rose-200 rounded-lg transition-all cursor-pointer"
            >
              <Trash2 size={15} />
            </button>
          </PermissionGate>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h2 className="font-heading text-2xl font-bold text-[#3b3036]">Gerenciar Produtos</h2>
        <div className="flex gap-3">
          <select
            value={filterActive === undefined ? 'ALL' : filterActive ? 'ACTIVE' : 'INACTIVE'}
            onChange={(e) => {
              const val = e.target.value;
              setFilterActive(val === 'ALL' ? undefined : val === 'ACTIVE');
            }}
            className="input-premium py-2 text-sm w-auto cursor-pointer"
          >
            <option value="ALL">Todos os Registros</option>
            <option value="ACTIVE">Apenas Ativos</option>
            <option value="INACTIVE">Apenas Inativos</option>
          </select>
          <PermissionGate method="POST" endpoint="/v1/products">
            <button onClick={() => handleOpenForm()} className="btn-premium">
              <Plus size={18} /> Novo Produto
            </button>
          </PermissionGate>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center gap-2 text-sm text-[#3b3036]/60 py-8">
          <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-[#be8a83]"></div>
          Carregando produtos...
        </div>
      ) : (
        <Table columns={columns} data={products} keyExtractor={(item) => item.id!} />
      )}

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title={editingProduct ? 'Editar Produto' : 'Novo Produto'}
        onSubmit={handleSubmit(onSubmit)}
      >
        <div className="space-y-4">
          <div>
            <label className={labelCls}>Nome do Produto</label>
            <input
              type="text"
              className={`${inputCls} ${errors.name ? 'border-rose-300' : ''}`}
              {...register('name', {
                required: 'Nome é obrigatório',
                minLength: { value: 3, message: 'Mín. 3 caracteres' },
              })}
            />
            {errors.name && (
              <span className="text-xs text-rose-500 font-semibold">{errors.name.message}</span>
            )}
          </div>
          <div>
            <label className={labelCls}>Estoque Inicial</label>
            <input
              type="number"
              className={`${inputCls} ${errors.stock ? 'border-rose-300' : ''}`}
              {...register('stock', {
                required: 'Estoque é obrigatório',
                min: { value: 0, message: 'Não pode ser negativo' },
              })}
            />
            {errors.stock && (
              <span className="text-xs text-rose-500 font-semibold">{errors.stock.message}</span>
            )}
          </div>
          <div>
            <label className={labelCls}>Preço (R$)</label>
            <input
              type="number"
              step="0.01"
              className={`${inputCls} ${errors.price ? 'border-rose-300' : ''}`}
              {...register('price', {
                required: 'Preço é obrigatório',
                min: { value: 0, message: 'Não pode ser negativo' },
              })}
            />
            {errors.price && (
              <span className="text-xs text-rose-500 font-semibold">{errors.price.message}</span>
            )}
          </div>
          <div className="flex items-center gap-3">
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" className="sr-only peer" {...register('active')} />
              <div className="w-10 h-5 bg-gray-200 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-[#be8a83]"></div>
            </label>
            <span className="text-sm font-semibold text-[#3b3036]">Produto Ativo</span>
          </div>
        </div>
      </ModalForm>

      <ConfirmDialog
        show={showConfirm}
        onHide={() => setShowConfirm(false)}
        onConfirm={confirmDelete}
        title="Excluir Produto"
        message="Tem certeza que deseja excluir este produto? Esta ação não pode ser desfeita."
      />
    </div>
  );
};
