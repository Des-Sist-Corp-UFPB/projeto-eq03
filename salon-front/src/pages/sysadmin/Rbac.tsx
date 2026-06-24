import { useState, useEffect, useCallback } from 'react';
import {
  Shield,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  XCircle,
  Plus,
  Minus,
  AlertCircle,
  RefreshCw,
  Lock,
  Unlock,
} from 'lucide-react';
import { rbacService, type RoleWithPermissions, type PermissionItem } from '../../services/rbac';
import { getApiErrorMessage } from '../../utils/apiError';
import { useAlert } from '../../hooks/useAlert';

// Método → cor visual
const METHOD_COLORS: Record<string, string> = {
  GET: 'bg-emerald-50 text-emerald-700 border-emerald-100',
  POST: 'bg-blue-50 text-blue-700 border-blue-100',
  PUT: 'bg-amber-50 text-amber-700 border-amber-100',
  PATCH: 'bg-amber-50 text-amber-700 border-amber-100',
  DELETE: 'bg-red-50 text-red-700 border-red-100',
};

const getMethodColor = (method: string) =>
  METHOD_COLORS[method.toUpperCase()] ?? 'bg-gray-50 text-gray-700 border-gray-100';

export const Rbac = () => {
  const [roles, setRoles] = useState<RoleWithPermissions[]>([]);
  const [allPermissions, setAllPermissions] = useState<PermissionItem[]>([]);
  const [expandedRoles, setExpandedRoles] = useState<Set<number>>(new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [togglingKey, setTogglingKey] = useState<string | null>(null);
  const [selectedClass, setSelectedClass] = useState<string>('all');

  const { error: showError, success: showSuccess } = useAlert();

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [rolesData, permsData] = await Promise.all([
        rbacService.getAllRoles(),
        rbacService.getAllPermissions(),
      ]);
      setRoles(rolesData);
      setAllPermissions(permsData);
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao carregar dados de RBAC.');
      showError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const toggleRole = (roleId: number) => {
    setExpandedRoles((prev) => {
      const next = new Set(prev);
      next.has(roleId) ? next.delete(roleId) : next.add(roleId);
      return next;
    });
  };

  const hasPermission = (role: RoleWithPermissions, permId: number) =>
    role.permissions.some((p) => p.id === permId);

  const handleTogglePermission = async (
    role: RoleWithPermissions,
    perm: PermissionItem
  ) => {
    const key = `${role.roleId}-${perm.id}`;
    setTogglingKey(key);
    try {
      const grant = !hasPermission(role, perm.id);
      const updated = grant
        ? await rbacService.grantPermission(role.roleId, perm.id)
        : await rbacService.revokePermission(role.roleId, perm.id);

      setRoles((prev) => prev.map((r) => (r.roleId === role.roleId ? updated : r)));
      showSuccess(
        grant
          ? `Permissão "${perm.name}" concedida a ${role.roleName} com sucesso.`
          : `Permissão "${perm.name}" revogada de ${role.roleName} com sucesso.`
      );
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Erro ao alterar permissão.');
      showError(msg);
    } finally {
      setTogglingKey(null);
    }
  };

  // Agrupamento de permissões por classe
  const classes = ['all', ...Array.from(new Set(allPermissions.map((p) => p.classe))).sort()];
  const filteredPermissions =
    selectedClass === 'all' ? allPermissions : allPermissions.filter((p) => p.classe === selectedClass);

  return (
    <div className="max-w-7xl mx-auto space-y-6 animate-fade-in-up">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <Shield size={32} className="text-[#be8a83]" />
          <div>
            <h2 className="font-heading text-2xl font-bold text-[#3b3036] tracking-wide">
              Painel de Permissões (RBAC)
            </h2>
            <p className="text-sm text-[#3b3036]/60 mt-1">
              Gerencie dinamicamente as permissões de cada cargo. As alterações têm efeito
              imediato no próximo login do usuário.
            </p>
          </div>
        </div>
        <button
          onClick={loadData}
          disabled={isLoading}
          className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-[#3b3036]/70 hover:text-[#be8a83] border border-[#eae1e1] rounded-full hover:border-[#be8a83]/40 transition-all duration-200 shrink-0 disabled:opacity-50"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin' : ''} />
          Atualizar
        </button>
      </div>

      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-24 gap-3">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-[#be8a83]" />
          <span className="text-sm text-[#3b3036]/60 font-medium">Carregando dados RBAC...</span>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Filtro por classe de permissão */}
          <div className="flex flex-wrap gap-2">
            {classes.map((cls) => (
              <button
                key={cls}
                onClick={() => setSelectedClass(cls)}
                className={`px-3 py-1.5 text-xs font-semibold rounded-full border transition-all duration-150 ${
                  selectedClass === cls
                    ? 'bg-[#be8a83] text-white border-[#be8a83]'
                    : 'bg-white text-[#3b3036]/70 border-[#eae1e1] hover:border-[#be8a83]/50 hover:text-[#be8a83]'
                }`}
              >
                {cls === 'all' ? 'Todas as classes' : cls}
              </button>
            ))}
          </div>

          {/* Lista de roles */}
          {roles.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 gap-2">
              <AlertCircle size={40} className="text-gray-300" />
              <span className="text-sm font-semibold text-[#3b3036]/80">Nenhum cargo encontrado.</span>
            </div>
          ) : (
            roles.map((role) => {
              const isExpanded = expandedRoles.has(role.roleId);
              const grantedCount = filteredPermissions.filter((p) => hasPermission(role, p.id)).length;
              const totalCount = filteredPermissions.length;

              return (
                <div
                  key={role.roleId}
                  className="bg-white rounded-2xl border border-[#eae1e1]/80 shadow-sm overflow-hidden transition-all duration-300"
                >
                  {/* Role Header */}
                  <button
                    onClick={() => toggleRole(role.roleId)}
                    className="w-full flex items-center justify-between px-6 py-4 hover:bg-[#be8a83]/3 transition-colors duration-150"
                    id={`role-toggle-${role.roleId}`}
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-[#be8a83] to-[#e5a49c] flex items-center justify-center text-white font-bold text-sm shadow-sm">
                        {role.roleName.charAt(0)}
                      </div>
                      <div className="text-left">
                        <h3 className="font-semibold text-[#3b3036] font-mono tracking-wide">
                          {role.roleName}
                        </h3>
                        <p className="text-xs text-[#3b3036]/50 mt-0.5">
                          {grantedCount} / {totalCount} permissões concedidas
                          {selectedClass !== 'all' && ` (filtro: ${selectedClass})`}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      {/* Progress bar */}
                      <div className="hidden sm:block w-28 bg-[#eae1e1] rounded-full h-1.5">
                        <div
                          className="bg-[#be8a83] h-1.5 rounded-full transition-all duration-500"
                          style={{ width: totalCount > 0 ? `${(grantedCount / totalCount) * 100}%` : '0%' }}
                        />
                      </div>
                      {isExpanded ? (
                        <ChevronUp size={18} className="text-[#3b3036]/40" />
                      ) : (
                        <ChevronDown size={18} className="text-[#3b3036]/40" />
                      )}
                    </div>
                  </button>

                  {/* Permissions Grid */}
                  {isExpanded && (
                    <div className="border-t border-[#eae1e1]/80 px-6 py-4">
                      {filteredPermissions.length === 0 ? (
                        <p className="text-sm text-[#3b3036]/50 py-4 text-center">
                          Nenhuma permissão encontrada para a classe selecionada.
                        </p>
                      ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
                          {filteredPermissions.map((perm) => {
                            const granted = hasPermission(role, perm.id);
                            const key = `${role.roleId}-${perm.id}`;
                            const isToggling = togglingKey === key;

                            return (
                              <div
                                key={perm.id}
                                className={`flex items-center justify-between gap-3 p-3 rounded-xl border transition-all duration-200 ${
                                  granted
                                    ? 'bg-emerald-50/50 border-emerald-100'
                                    : 'bg-[#fcf9f9] border-[#eae1e1]/80'
                                }`}
                              >
                                <div className="min-w-0">
                                  <div className="flex items-center gap-2 flex-wrap">
                                    <span
                                      className={`inline-flex items-center px-1.5 py-0.5 text-[10px] font-bold rounded border font-mono ${getMethodColor(perm.httpMethod)}`}
                                    >
                                      {perm.httpMethod}
                                    </span>
                                    <span className="text-xs font-mono text-[#3b3036]/80 truncate">
                                      {perm.endpoint}
                                    </span>
                                  </div>
                                  <p className="text-[11px] text-[#3b3036]/55 mt-0.5 truncate" title={perm.name}>
                                    {perm.name}
                                  </p>
                                </div>

                                <button
                                  onClick={() => handleTogglePermission(role, perm)}
                                  disabled={isToggling}
                                  id={`perm-toggle-${role.roleId}-${perm.id}`}
                                  title={granted ? 'Revogar permissão' : 'Conceder permissão'}
                                  className={`shrink-0 p-1.5 rounded-lg transition-all duration-200 disabled:opacity-50 ${
                                    granted
                                      ? 'text-emerald-600 hover:bg-red-50 hover:text-red-500'
                                      : 'text-[#3b3036]/30 hover:bg-emerald-50 hover:text-emerald-600'
                                  }`}
                                >
                                  {isToggling ? (
                                    <div className="w-4 h-4 rounded-full border-2 border-current border-t-transparent animate-spin" />
                                  ) : granted ? (
                                    <CheckCircle2 size={18} />
                                  ) : (
                                    <XCircle size={18} />
                                  )}
                                </button>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })
          )}

          {/* Legenda */}
          <div className="flex flex-wrap items-center gap-4 text-xs text-[#3b3036]/50 pt-2 border-t border-[#eae1e1]/60">
            <div className="flex items-center gap-1.5">
              <CheckCircle2 size={13} className="text-emerald-500" />
              <span>Permissão concedida (clique para revogar)</span>
            </div>
            <div className="flex items-center gap-1.5">
              <XCircle size={13} className="text-[#3b3036]/30" />
              <span>Permissão não concedida (clique para conceder)</span>
            </div>
            <div className="flex items-center gap-1.5 ml-auto">
              <Lock size={13} className="text-amber-500" />
              <span>Alterações têm efeito no próximo login</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
