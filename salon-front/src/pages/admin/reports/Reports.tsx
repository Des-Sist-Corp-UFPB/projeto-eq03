import { useState, useEffect } from 'react';
import { reportsApi } from './services/reports';
import type { FinancialReportResponse, AppointmentReportResponse } from './services/reports';
import { cashFlowApi } from '../cashflow/services/cashflow';
import type { CashFlowData } from '../cashflow/services/cashflow';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, Legend } from 'recharts';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { Download } from 'lucide-react';
import { useAlert } from '../../../hooks/useAlert';
import { getApiErrorMessage } from '../../../utils/apiError';

const inputCls = 'input-premium';
const labelCls = 'label-premium';

export const Reports = () => {
  const [financial, setFinancial] = useState<FinancialReportResponse | null>(null);
  const [appointments, setAppointments] = useState<AppointmentReportResponse | null>(null);
  const [cashFlows, setCashFlows] = useState<CashFlowData[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const { error: showError } = useAlert();

  const loadReports = async () => {
    setIsLoading(true);
    try {
      const [finData, aptData, cfData] = await Promise.all([
        reportsApi.getFinancialReport(dateFrom || undefined, dateTo || undefined),
        reportsApi.getAppointmentReport(dateFrom || undefined, dateTo || undefined),
        cashFlowApi.findByPeriod(dateFrom || undefined, dateTo || undefined)
      ]);
      setFinancial(finData);
      setAppointments(aptData);
      setCashFlows(cfData);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao carregar relatórios');
      await showError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { loadReports(); }, [dateFrom, dateTo]);

  const generatePDF = () => {
    const doc = new jsPDF();
    doc.setFontSize(18);
    doc.text('Relatório Financeiro e Agendamentos', 14, 22);
    doc.setFontSize(11);
    doc.text(`Período: ${financial?.period || 'N/A'}`, 14, 30);
    
    doc.setFontSize(14);
    doc.text('Resumo Financeiro', 14, 45);
    autoTable(doc, {
      startY: 50,
      head: [['Receitas', 'Despesas Gerais', 'Gastos Salários', 'Gastos Comissões', 'Lucro Líquido']],
      body: [[
        `R$ ${(financial?.totalIncome ?? 0).toFixed(2)}`,
        `R$ ${(financial?.totalExpense ?? 0).toFixed(2)}`,
        `R$ ${(financial?.totalSalaryPaid ?? 0).toFixed(2)}`,
        `R$ ${(financial?.totalCommissionPaid ?? 0).toFixed(2)}`,
        `R$ ${(financial?.netProfit ?? 0).toFixed(2)}`
      ]],
    });
    
    let currentY = (doc as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY || 50;
    
    if (financial?.employeeFinanceDetails && financial.employeeFinanceDetails.length > 0) {
      doc.setFontSize(14);
      doc.text('Detalhamento de Remunerações por Funcionária', 14, currentY + 15);
      
      const employeeRows = financial.employeeFinanceDetails.map(emp => {
        let typeStr = 'Não definido';
        let baseStr = 'R$ 0.00';
        
        if (emp.remunerationType === 'SALARIO_FIXO') {
          typeStr = 'Salário Fixo';
          baseStr = `R$ ${(emp.remunerationValue ?? 0).toFixed(2)}`;
        } else if (emp.remunerationType === 'COMISSIONADO') {
          typeStr = 'Comissionado';
          const scopeStr = emp.commissionScope === 'GLOBAL' ? 'Global' : 'Individual';
          baseStr = `${emp.remunerationValue ?? 0}% (${scopeStr})`;
        } else if (emp.remunerationType === 'FIXO_E_COMISSIONADO') {
          typeStr = 'Fixo + Comissionado';
          const scopeStr = emp.commissionScope === 'GLOBAL' ? 'Global' : 'Individual';
          baseStr = `R$ ${(emp.remunerationValue ?? 0).toFixed(2)} + ${(emp.commissionValue ?? 0).toFixed(0)}% (${scopeStr})`;
        }
        
        return [
          emp.employeeName,
          typeStr,
          baseStr,
          emp.doneAppointmentsCount.toString(),
          `R$ ${(emp.doneAppointmentsValue ?? 0).toFixed(2)}`,
          `R$ ${(emp.calculatedPayout ?? 0).toFixed(2)}`
        ];
      });
      
      autoTable(doc, {
        startY: currentY + 20,
        head: [['Nome', 'Tipo', 'Base', 'Atendimentos', 'Valor Atendimentos', 'Valor A Pagar']],
        body: employeeRows
      });
      
      currentY = (doc as { lastAutoTable?: { finalY?: number } }).lastAutoTable?.finalY || currentY + 20;
    }
    
    doc.setFontSize(14);
    doc.text('Resumo de Agendamentos', 14, currentY + 15);
    autoTable(doc, {
      startY: currentY + 20,
      head: [['Total', 'Concluídos', 'Pendentes', 'Cancelados']],
      body: [[
        appointments?.totalAppointments || 0,
        appointments?.done || 0,
        appointments?.pending || 0,
        appointments?.cancelled || 0
      ]],
    });
    doc.save('relatorio-salao.pdf');
  };

  const chartDataEmployee = Object.entries(appointments?.byEmployee || {}).map(([name, count]) => ({ name, Agendamentos: count }));
  const chartDataService = Object.entries(appointments?.byService || {}).map(([name, count]) => ({ name, Agendamentos: count }));

  const revenueByDate: Record<string, number> = {};
  cashFlows.forEach(cf => {
    if (cf.type === 'INCOME') {
      const d = cf.date.substring(0, 10);
      revenueByDate[d] = (revenueByDate[d] || 0) + cf.amount;
    }
  });
  const chartDataRevenue = Object.entries(revenueByDate)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, amount]) => ({
      Data: new Date(date).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
      Receita: amount
    }));

  return (
    <div className="space-y-8 animate-fade-in-up">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h2 className="font-heading text-2xl font-bold text-[#3b3036]">Dashboard & Relatórios</h2>
        <button
          onClick={generatePDF}
          disabled={isLoading}
          className="flex items-center gap-2 px-5 py-2.5 bg-[#be8a83] hover:bg-[#a1706a] text-white font-semibold text-sm rounded-xl transition-all shadow-md shadow-[#be8a83]/10 disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
        >
          <Download size={18} /> Exportar PDF
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-4 items-end bg-white/80 backdrop-blur-md rounded-2xl border border-[#eae1e1]/80 p-5 shadow-sm">
        <div className="space-y-1">
          <label className={labelCls}>De</label>
          <input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} className={inputCls} />
        </div>
        <div className="space-y-1">
          <label className={labelCls}>Até</label>
          <input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} className={inputCls} />
        </div>
        <button
          onClick={() => { setDateFrom(''); setDateTo(''); }}
          className="px-5 py-2.5 border border-[#eae1e1] text-sm font-semibold text-[#3b3036] hover:text-[#be8a83] hover:border-[#be8a83] bg-white rounded-xl transition-all duration-200 cursor-pointer"
        >
          Mês Atual (Padrão)
        </button>
      </div>

      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-20 gap-3">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-[#be8a83]"></div>
          <p className="text-sm text-[#3b3036]/60 font-medium">Gerando relatórios...</p>
        </div>
      ) : (
        <>
          {/* Financial Summary */}
          <div className="space-y-6">
            <h4 className="font-heading font-bold text-lg text-[#3b3036]">
              Resumo Financeiro <span className="text-sm font-normal text-gray-400">({financial?.period})</span>
            </h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
              <div className="bg-white rounded-2xl border border-[#eae1e1]/80 p-5 shadow-sm transition-all duration-300 relative overflow-hidden group">
                <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-emerald-400 to-emerald-500" />
                <p className="text-[10px] font-bold text-[#7a7074] uppercase tracking-wider mb-1">Total Receitas</p>
                <p className="text-xl font-extrabold text-emerald-600">R$ {(financial?.totalIncome ?? 0).toFixed(2)}</p>
              </div>
              <div className="bg-white rounded-2xl border border-[#eae1e1]/80 p-5 shadow-sm transition-all duration-300 relative overflow-hidden group">
                <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-rose-400 to-rose-500" />
                <p className="text-[10px] font-bold text-[#7a7074] uppercase tracking-wider mb-1">Despesas Gerais</p>
                <p className="text-xl font-extrabold text-rose-600">R$ {(financial?.totalExpense ?? 0).toFixed(2)}</p>
              </div>
              <div className="bg-white rounded-2xl border border-[#eae1e1]/80 p-5 shadow-sm transition-all duration-300 relative overflow-hidden group">
                <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-indigo-400 to-indigo-500" />
                <p className="text-[10px] font-bold text-[#7a7074] uppercase tracking-wider mb-1">Salários Fixos</p>
                <p className="text-xl font-extrabold text-indigo-600">R$ {(financial?.totalSalaryPaid ?? 0).toFixed(2)}</p>
              </div>
              <div className="bg-white rounded-2xl border border-[#eae1e1]/80 p-5 shadow-sm transition-all duration-300 relative overflow-hidden group">
                <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-amber-400 to-amber-500" />
                <p className="text-[10px] font-bold text-[#7a7074] uppercase tracking-wider mb-1">Comissões</p>
                <p className="text-xl font-extrabold text-amber-600">R$ {(financial?.totalCommissionPaid ?? 0).toFixed(2)}</p>
              </div>
              <div className="bg-white rounded-2xl border border-[#eae1e1]/80 p-5 shadow-sm transition-all duration-300 relative overflow-hidden group">
                <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-[#be8a83] to-[#e5a49c]" />
                <p className="text-[10px] font-bold text-[#7a7074] uppercase tracking-wider mb-1">Lucro Líquido</p>
                <p className={`text-xl font-extrabold ${(financial?.netProfit ?? 0) >= 0 ? 'text-[#be8a83]' : 'text-rose-600'}`}>
                  R$ {(financial?.netProfit ?? 0).toFixed(2)}
                </p>
              </div>
            </div>
          </div>

          {/* Employee Remuneration breakdown table */}
          {financial?.employeeFinanceDetails && financial.employeeFinanceDetails.length > 0 && (
            <div className="space-y-4">
              <h4 className="font-heading font-bold text-lg text-[#3b3036]">
                Detalhamento de Remunerações por Funcionária
              </h4>
              <div className="bg-white rounded-2xl border border-[#eae1e1]/80 shadow-sm overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse min-w-[600px]">
                    <thead>
                      <tr className="bg-[#fcf9f9]/50 border-b border-[#eae1e1]">
                        <th className="px-6 py-4 text-xs font-semibold text-[#3b3036] font-heading uppercase tracking-wider">Nome</th>
                        <th className="px-6 py-4 text-xs font-semibold text-[#3b3036] font-heading uppercase tracking-wider">Modelo</th>
                        <th className="px-6 py-4 text-xs font-semibold text-[#3b3036] font-heading uppercase tracking-wider">Base</th>
                        <th className="px-6 py-4 text-xs font-semibold text-[#3b3036] font-heading uppercase tracking-wider text-center">Atendimentos</th>
                        <th className="px-6 py-4 text-xs font-semibold text-[#3b3036] font-heading uppercase tracking-wider text-right">Valor Atendimentos</th>
                        <th className="px-6 py-4 text-xs font-semibold text-[#3b3036] font-heading uppercase tracking-wider text-right">Valor A Pagar</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#eae1e1]/40">
                      {financial.employeeFinanceDetails.map((emp) => {
                        let typeStr = 'Não definido';
                        let baseStr = 'R$ 0,00';
                        
                        if (emp.remunerationType === 'SALARIO_FIXO') {
                          typeStr = 'Salário Fixo';
                          baseStr = `R$ ${(emp.remunerationValue ?? 0).toFixed(2)}`;
                        } else if (emp.remunerationType === 'COMISSIONADO') {
                          typeStr = 'Comissionado';
                          const scopeStr = emp.commissionScope === 'GLOBAL' ? 'Global' : 'Individual';
                          baseStr = `${emp.remunerationValue ?? 0}% (${scopeStr})`;
                        } else if (emp.remunerationType === 'FIXO_E_COMISSIONADO') {
                          typeStr = 'Fixo + Comissionado';
                          const scopeStr = emp.commissionScope === 'GLOBAL' ? 'Global' : 'Individual';
                          baseStr = `R$ ${(emp.remunerationValue ?? 0).toFixed(2)} + ${(emp.commissionValue ?? 0).toFixed(0)}% (${scopeStr})`;
                        }

                        return (
                          <tr key={emp.employeeId} className="hover:bg-[#fcf9f9]/20 transition-colors">
                            <td className="px-6 py-4 text-sm font-semibold text-[#3b3036]">{emp.employeeName}</td>
                            <td className="px-6 py-4 text-sm text-[#7a7074]">{typeStr}</td>
                            <td className="px-6 py-4 text-sm text-[#7a7074]">{baseStr}</td>
                            <td className="px-6 py-4 text-sm text-[#7a7074] text-center">{emp.doneAppointmentsCount}</td>
                            <td className="px-6 py-4 text-sm text-[#7a7074] text-right">R$ ${(emp.doneAppointmentsValue ?? 0).toFixed(2)}</td>
                            <td className="px-6 py-4 text-sm font-bold text-[#be8a83] text-right">R$ ${(emp.calculatedPayout ?? 0).toFixed(2)}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* Revenue Chart */}
          <div>
            <h4 className="font-heading font-bold text-lg text-[#3b3036] mb-4">Evolução de Receitas</h4>
            <div className="bg-white rounded-2xl border border-[#eae1e1]/80 p-6 shadow-sm">
              <div style={{ height: 300, width: '100%' }}>
                {chartDataRevenue.length > 0 ? (
                  <ResponsiveContainer>
                    <LineChart data={chartDataRevenue}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                      <XAxis dataKey="Data" tick={{ fontSize: 12 }} />
                      <YAxis tick={{ fontSize: 12 }} />
                      <Tooltip formatter={(val: any) => `R$ ${Number(val ?? 0).toFixed(2)}`} />
                      <Legend />
                      <Line type="monotone" dataKey="Receita" stroke="#be8a83" strokeWidth={2} activeDot={{ r: 8 }} />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex items-center justify-center h-full text-sm text-gray-400">
                    Nenhuma receita no período selecionado.
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Appointment Stats */}
          <div>
            <h4 className="font-heading font-bold text-lg text-[#3b3036] mb-4">
              Desempenho de Agendamentos <span className="text-sm font-normal text-gray-400">({appointments?.period})</span>
            </h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-5 mb-8">
              {[
                { label: 'Total', value: appointments?.totalAppointments, color: 'text-indigo-600', stripe: 'from-indigo-400 to-indigo-500' },
                { label: 'Concluídos', value: appointments?.done, color: 'text-emerald-600', stripe: 'from-emerald-400 to-emerald-500' },
                { label: 'Pendentes', value: appointments?.pending, color: 'text-amber-600', stripe: 'from-amber-400 to-amber-500' },
                { label: 'Cancelados', value: appointments?.cancelled, color: 'text-rose-600', stripe: 'from-rose-400 to-rose-500' },
              ].map((stat, i) => (
                <div key={i} className="bg-white rounded-2xl border border-[#eae1e1]/80 p-5 text-center shadow-sm relative overflow-hidden group transition-all duration-300">
                  <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${stat.stripe}`} />
                  <p className={`text-4xl font-extrabold ${stat.color}`}>{stat.value}</p>
                  <p className="text-xs font-semibold text-[#7a7074] uppercase tracking-wider mt-1">{stat.label}</p>
                </div>
              ))}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="bg-white rounded-2xl border border-[#eae1e1]/80 shadow-sm overflow-hidden">
                <div className="px-5 py-4 border-b border-[#eae1e1] bg-[#fcf9f9]/50">
                  <span className="font-semibold text-xs text-[#3b3036] font-heading uppercase tracking-wider">Por Profissional</span>
                </div>
                <div className="p-4" style={{ height: 300 }}>
                  {chartDataEmployee.length > 0 ? (
                    <ResponsiveContainer>
                      <BarChart data={chartDataEmployee}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                        <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                        <YAxis tick={{ fontSize: 11 }} />
                        <Tooltip />
                        <Bar dataKey="Agendamentos" fill="#be8a83" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="flex items-center justify-center h-full text-sm text-gray-400">Nenhum dado</div>
                  )}
                </div>
              </div>
              <div className="bg-white rounded-2xl border border-[#eae1e1]/80 shadow-sm overflow-hidden">
                <div className="px-5 py-4 border-b border-[#eae1e1] bg-[#fcf9f9]/50">
                  <span className="font-semibold text-xs text-[#3b3036] font-heading uppercase tracking-wider">Por Serviço</span>
                </div>
                <div className="p-4" style={{ height: 300 }}>
                  {chartDataService.length > 0 ? (
                    <ResponsiveContainer>
                      <BarChart data={chartDataService} layout="vertical">
                        <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                        <XAxis type="number" tick={{ fontSize: 11 }} />
                        <YAxis dataKey="name" type="category" width={100} tick={{ fontSize: 11 }} />
                        <Tooltip />
                        <Bar dataKey="Agendamentos" fill="#3b3036" radius={[0, 4, 4, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="flex items-center justify-center h-full text-sm text-gray-400">Nenhum dado</div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
