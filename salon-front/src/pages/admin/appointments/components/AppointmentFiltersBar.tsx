import type { EmployeeData } from '../../employees/services/employees';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export interface AppointmentFiltersState {
  status: string;
  employeeId: string;
  clientName: string;
  startDate: string;
  endDate: string;
}

export const emptyAppointmentFilters: AppointmentFiltersState = {
  status: '',
  employeeId: '',
  clientName: '',
  startDate: '',
  endDate: '',
};

const statusOptions = [
  { value: 'PENDING', label: 'Pendente' },
  { value: 'REQUESTED', label: 'Solicitado' },
  { value: 'CONFIRMED', label: 'Confirmado' },
  { value: 'DECLINED', label: 'Recusado' },
  { value: 'DONE', label: 'Concluído' },
  { value: 'CANCELLED', label: 'Cancelado' },
];

interface AppointmentFiltersBarProps {
  filters: AppointmentFiltersState;
  employees: EmployeeData[];
  onChange: (patch: Partial<AppointmentFiltersState>) => void;
  onClear: () => void;
}

export function countActiveFilters(filters: AppointmentFiltersState): number {
  return Object.values(filters).filter((v) => v !== '').length;
}

export const AppointmentFiltersBar = ({ filters, employees, onChange, onClear }: AppointmentFiltersBarProps) => {
  const activeCount = countActiveFilters(filters);

  return (
    <div className="flex flex-wrap gap-4 items-end bg-white/80 backdrop-blur-md rounded-2xl border border-[#eae1e1]/80 p-5 shadow-sm">
      <div className="space-y-1 min-w-[160px] flex-1">
        <label className={labelCls}>Status</label>
        <select
          className={inputCls}
          value={filters.status}
          onChange={(e) => onChange({ status: e.target.value })}
        >
          <option value="">Todos</option>
          {statusOptions.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      <div className="space-y-1 min-w-[180px] flex-1">
        <label className={labelCls}>Profissional</label>
        <select
          className={inputCls}
          value={filters.employeeId}
          onChange={(e) => onChange({ employeeId: e.target.value })}
        >
          <option value="">Todas</option>
          {employees.map((e) => (
            <option key={e.id} value={e.id}>
              {e.name}
            </option>
          ))}
        </select>
      </div>

      <div className="space-y-1 min-w-[180px] flex-1">
        <label className={labelCls}>Cliente</label>
        <input
          type="text"
          placeholder="Buscar por nome..."
          className={inputCls}
          value={filters.clientName}
          onChange={(e) => onChange({ clientName: e.target.value })}
        />
      </div>

      <div className="space-y-1">
        <label className={labelCls}>De</label>
        <input
          type="date"
          className={inputCls}
          value={filters.startDate}
          onChange={(e) => onChange({ startDate: e.target.value })}
        />
      </div>

      <div className="space-y-1">
        <label className={labelCls}>Até</label>
        <input
          type="date"
          className={inputCls}
          value={filters.endDate}
          onChange={(e) => onChange({ endDate: e.target.value })}
        />
      </div>

      <button
        onClick={onClear}
        className="px-5 py-2.5 border border-[#eae1e1] text-sm font-semibold text-[#3b3036] hover:text-[#be8a83] hover:border-[#be8a83] bg-white rounded-xl transition-all duration-200 cursor-pointer flex items-center gap-2"
      >
        Limpar Filtros
        {activeCount > 0 && (
          <span className="inline-flex items-center justify-center w-5 h-5 text-[11px] font-bold rounded-full bg-[#be8a83] text-white">
            {activeCount}
          </span>
        )}
      </button>
    </div>
  );
};
