import { useState, useEffect } from 'react';
import { Button, Form } from 'react-bootstrap';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Trash2 } from 'lucide-react';
import { Table } from '../../../components/table/Table';
import { ModalForm } from '../../../components/modal/ModalForm';
import { ConfirmDialog } from '../../../components/modal/ConfirmDialog';
import { PermissionGate } from '../../../components/permissions/PermissionGate';
import { productsApi, ProductData } from './services/products';

export const Products = () => {
  const [products, setProducts] = useState<ProductData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [showForm, setShowForm] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ProductData | null>(null);
  
  const [showConfirm, setShowConfirm] = useState(false);
  const [productToDelete, setProductToDelete] = useState<number | null>(null);
  
  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<ProductData>();

  const loadProducts = async () => {
    setIsLoading(true);
    try {
      const data = await productsApi.findAll();
      setProducts(data);
    } catch (error) {
      console.error('Erro ao carregar produtos', error);
      alert('Erro ao carregar produtos');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const handleOpenForm = (product?: ProductData) => {
    reset();
    if (product) {
      setEditingProduct(product);
      setValue('name', product.name);
      setValue('stock', product.stock);
      setValue('price', product.price);
    } else {
      setEditingProduct(null);
      setValue('stock', 0);
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
    } catch (error) {
      console.error('Erro ao salvar produto', error);
      alert('Erro ao salvar produto. Verifique os dados e tente novamente.');
    }
  };

  const confirmDelete = async () => {
    if (!productToDelete) return;
    try {
      await productsApi.delete(productToDelete);
      setShowConfirm(false);
      loadProducts();
    } catch (error) {
      console.error('Erro ao excluir produto', error);
      alert('Erro ao excluir produto.');
    }
  };

  const columns = [
    { key: 'name', label: 'Nome do Produto' },
    { 
      key: 'price', 
      label: 'Preço',
      render: (item: ProductData) => `R$ ${item.price.toFixed(2)}`
    },
    { key: 'stock', label: 'Estoque' },
    {
      key: 'actions',
      label: 'Ações',
      render: (item: ProductData) => (
        <div className="d-flex gap-2">
          <PermissionGate method="PUT" endpoint={`/v1/products/${item.id}`}>
            <Button variant="outline-primary" size="sm" onClick={() => handleOpenForm(item)}>
              <Edit size={16} />
            </Button>
          </PermissionGate>
          
          <PermissionGate method="DELETE" endpoint={`/v1/products/${item.id}`}>
            <Button 
              variant="outline-danger" 
              size="sm" 
              onClick={() => {
                setProductToDelete(item.id!);
                setShowConfirm(true);
              }}
            >
              <Trash2 size={16} />
            </Button>
          </PermissionGate>
        </div>
      )
    }
  ];

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Gerenciar Produtos</h2>
        <PermissionGate method="POST" endpoint="/v1/products">
          <Button variant="primary" onClick={() => handleOpenForm()}>
            <Plus size={18} className="me-2" />
            Novo Produto
          </Button>
        </PermissionGate>
      </div>

      {isLoading ? (
        <p>Carregando produtos...</p>
      ) : (
        <Table 
          columns={columns} 
          data={products} 
          keyExtractor={(item) => item.id!} 
        />
      )}

      <ModalForm
        show={showForm}
        onHide={() => setShowForm(false)}
        title={editingProduct ? 'Editar Produto' : 'Novo Produto'}
        onSubmit={handleSubmit(onSubmit)}
      >
        <Form.Group className="mb-3">
          <Form.Label>Nome do Produto</Form.Label>
          <Form.Control 
            type="text" 
            {...register('name', { required: 'Nome é obrigatório', minLength: { value: 3, message: 'Mín. 3 caracteres'} })}
            isInvalid={!!errors.name}
          />
          <Form.Control.Feedback type="invalid">{errors.name?.message}</Form.Control.Feedback>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Estoque Inicial</Form.Label>
          <Form.Control 
            type="number" 
            {...register('stock', { required: 'Estoque é obrigatório', min: { value: 0, message: 'Não pode ser negativo'} })}
            isInvalid={!!errors.stock}
          />
          <Form.Control.Feedback type="invalid">{errors.stock?.message}</Form.Control.Feedback>
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Preço (R$)</Form.Label>
          <Form.Control 
            type="number" 
            step="0.01" 
            {...register('price', { required: 'Preço é obrigatório', min: { value: 0, message: 'Não pode ser negativo'} })}
            isInvalid={!!errors.price}
          />
          <Form.Control.Feedback type="invalid">{errors.price?.message}</Form.Control.Feedback>
        </Form.Group>
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
