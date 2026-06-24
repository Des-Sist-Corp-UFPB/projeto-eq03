import { useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { Table } from './Table';
import { SearchX } from 'lucide-react';

export interface FilterField {
  key: string;
  label: string;
  type: 'text' | 'select' | 'boolean';
  options?: { value: string | number; label: string }[];
}

interface Column<T> {
  key: keyof T | string;
  label: string;
  render?: (item: T) => ReactNode;
}

interface DataTableProps<T, F> {
  columns: Column<T>[];
  fetchData: (filter: F, page: number, size: number) => Promise<{ content: T[]; totalPages: number }>;
  filtersConfig?: FilterField[];
  keyExtractor: (item: T) => string | number;
  onRowClick?: (item: T) => void;
  refreshTrigger?: any;
  initialFilters: F;
}

export function DataTable<T, F>({
  columns,
  fetchData,
  filtersConfig = [],
  keyExtractor,
  onRowClick,
  refreshTrigger,
  initialFilters,
}: DataTableProps<T, F>) {
  const [data, setData] = useState<T[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [filters, setFilters] = useState<F>(initialFilters);
  const [isLoading, setIsLoading] = useState(true);

  const loadData = async () => {
    setIsLoading(true);
    try {
      // Backend expects 0-indexed page
      const response = await fetchData(filters, currentPage - 1, 10);
      setData(response.content || []);
      setTotalPages(response.totalPages || 1);
    } catch (error) {
      console.error('Erro ao buscar dados na DataTable:', error);
      setData([]);
      setTotalPages(1);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [currentPage, filters, refreshTrigger]);

  const handleFilterChange = (key: string, value: any) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value === '' ? undefined : value,
    }));
    setCurrentPage(1); // reset to page 1 on filter change
  };

  return (
    <div className="space-y-6">
      {/* Dynamic Filter Row */}
      {filtersConfig.length > 0 && (
        <div className="bg-white/80 dark:bg-[#161c2a] backdrop-blur-md rounded-2xl p-6 border border-[#eae1e1]/80 dark:border-[#1e293b] shadow-sm flex flex-wrap gap-4 items-end animate-fade-in-up">
          {filtersConfig.map((field) => {
            const val = (filters as any)[field.key] ?? '';
            return (
              <div key={field.key} className="flex flex-col gap-1.5 min-w-[200px] flex-1">
                <label className="text-xs font-bold text-[#7a7074] dark:text-gray-400 uppercase tracking-wider">
                  {field.label}
                </label>
                {field.type === 'text' && (
                  <input
                    type="text"
                    value={val}
                    onChange={(e) => handleFilterChange(field.key, e.target.value)}
                    placeholder={`Filtrar por ${field.label.toLowerCase()}...`}
                    className="input-premium py-2.5 px-3 text-sm"
                  />
                )}
                {field.type === 'select' && (
                  <select
                    value={val}
                    onChange={(e) => handleFilterChange(field.key, e.target.value)}
                    className="input-premium py-2.5 px-3 text-sm"
                  >
                    <option value="">Todos</option>
                    {field.options?.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                )}
                {field.type === 'boolean' && (
                  <select
                    value={val === '' ? '' : String(val)}
                    onChange={(e) => {
                      const v = e.target.value;
                      handleFilterChange(
                        field.key,
                        v === 'true' ? true : v === 'false' ? false : ''
                      );
                    }}
                    className="input-premium py-2.5 px-3 text-sm"
                  >
                    <option value="">Todos</option>
                    <option value="true">Ativo</option>
                    <option value="false">Inativo</option>
                  </select>
                )}
              </div>
            );
          })}
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 text-sm text-[#3b3036]/60 py-12">
          <div className="animate-spin rounded-full h-6 w-6 border-t-2 border-b-2 border-[#be8a83]"></div>
          <span>Carregando registros...</span>
        </div>
      ) : data.length === 0 ? (
        /* Elegant Empty State */
        <div className="w-full bg-white/80 dark:bg-[#161c2a] backdrop-blur-md rounded-2xl p-12 border border-[#eae1e1]/80 dark:border-[#1e293b] shadow-sm flex flex-col items-center justify-center text-center space-y-4 animate-fade-in-up">
          <div className="w-14 h-14 bg-[#be8a83]/10 dark:bg-[#e5a49c]/10 rounded-full flex items-center justify-center text-[#be8a83] dark:text-[#e5a49c]">
            <SearchX size={26} />
          </div>
          <div className="space-y-1">
            <h4 className="font-heading text-lg font-bold text-[#3b3036] dark:text-white m-0">
              Nenhum resultado encontrado
            </h4>
            <p className="text-sm text-[#7a7074] dark:text-gray-400 max-w-md mx-auto leading-relaxed">
              Não encontramos nenhum registro que corresponda aos filtros de busca aplicados. Tente limpar os filtros ou digitar outros termos.
            </p>
          </div>
        </div>
      ) : (
        <Table
          columns={columns}
          data={data}
          keyExtractor={keyExtractor}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
          onRowClick={onRowClick}
        />
      )}
    </div>
  );
}
