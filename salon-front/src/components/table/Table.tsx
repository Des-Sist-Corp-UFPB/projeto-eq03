import type { ReactNode } from 'react';
import { Search } from 'lucide-react';

interface Column<T> {
  key: keyof T | string;
  label: string;
  render?: (item: T) => ReactNode;
}

interface TableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (item: T) => string | number;
  searchTerm?: string;
  onSearchChange?: (term: string) => void;
  currentPage?: number;
  totalPages?: number;
  onPageChange?: (page: number) => void;
  emptyMessage?: string;
}

export function Table<T>({
  columns,
  data,
  keyExtractor,
  searchTerm,
  onSearchChange,
  currentPage = 1,
  totalPages = 1,
  onPageChange,
  emptyMessage = 'Nenhum registro encontrado.'
}: TableProps<T>) {
  return (
    <div className="w-full bg-white/80 backdrop-blur-md rounded-2xl p-6 border border-[#eae1e1]/80 shadow-sm mb-8 animate-fade-in-up">
      {onSearchChange && (
        <div className="mb-4 flex justify-end">
          <div className="relative w-full max-w-xs">
            <span className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none text-[#7a7074]">
              <Search size={18} />
            </span>
            <input
              type="text"
              placeholder="Buscar..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              className="input-premium pl-10 pr-4 py-2"
            />
          </div>
        </div>
      )}

      <div className="w-full overflow-x-auto">
        <table className="w-full text-left border-separate border-spacing-y-2">
          <thead>
            <tr className="text-[#7a7074] font-semibold text-xs uppercase tracking-wider">
              {columns.map((col, index) => (
                <th key={String(col.key) + index} className="px-4 py-3 pb-4">
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="text-sm text-[#2a2528]">
            {data.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="px-4 py-8 text-center text-[#7a7074]/80 bg-white/50 border border-[#eae1e1] rounded-xl">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              data.map((item) => (
                <tr key={keyExtractor(item)} className="group">
                  {columns.map((col, index) => {
                    const isFirst = index === 0;
                    const isLast = index === columns.length - 1;
                    return (
                      <td
                        key={String(col.key) + index}
                        className={`px-4 py-4 align-middle bg-white/60 border-y border-[#eae1e1] group-hover:bg-[#fcf9f9] transition-all duration-200 ${
                          isFirst ? 'border-l rounded-l-xl' : ''
                        } ${isLast ? 'border-r rounded-r-xl' : ''}`}
                      >
                        {col.render ? col.render(item) : String(item[col.key as keyof T] || '')}
                      </td>
                    );
                  })}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && onPageChange && (
        <div className="flex justify-center items-center gap-2 mt-6">
          <button
            disabled={currentPage === 1}
            onClick={() => onPageChange(currentPage - 1)}
            className="px-4 py-2 border border-[#eae1e1] rounded-lg bg-white text-[#3b3036] hover:text-[#be8a83] hover:border-[#be8a83] disabled:opacity-50 disabled:cursor-not-allowed transition-all text-sm font-medium"
          >
            Anterior
          </button>
          
          <div className="flex items-center gap-1">
            {Array.from({ length: totalPages }).map((_, i) => {
              const pageNum = i + 1;
              const isActive = pageNum === currentPage;
              return (
                <button
                  key={pageNum}
                  onClick={() => onPageChange(pageNum)}
                  className={`w-9 h-9 rounded-lg flex items-center justify-center text-sm font-semibold transition-all ${
                    isActive
                      ? 'bg-[#be8a83] text-white shadow-sm'
                      : 'border border-[#eae1e1] bg-white text-[#3b3036] hover:text-[#be8a83] hover:border-[#be8a83]'
                  }`}
                >
                  {pageNum}
                </button>
              );
            })}
          </div>

          <button
            disabled={currentPage === totalPages}
            onClick={() => onPageChange(currentPage + 1)}
            className="px-4 py-2 border border-[#eae1e1] rounded-lg bg-white text-[#3b3036] hover:text-[#be8a83] hover:border-[#be8a83] disabled:opacity-50 disabled:cursor-not-allowed transition-all text-sm font-medium"
          >
            Próxima
          </button>
        </div>
      )}
    </div>
  );
}
