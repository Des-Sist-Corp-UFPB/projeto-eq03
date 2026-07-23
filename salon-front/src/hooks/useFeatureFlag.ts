import { useEffect, useState } from 'react';
import { featureFlagsService } from '../services/featureFlags';

/**
 * Consulta o estado de uma feature flag pública (GET /v1/feature-flags) — não exige
 * permissão de sysadmin, então pode ser usado por qualquer tela autenticada que
 * precise saber se uma feature está ligada antes de renderizar algo dependente dela.
 *
 * Em caso de falha ao consultar (rede fora do ar, etc.), assume DESATIVADA: features
 * atrás de flag normalmente ainda estão em validação, então esconder por segurança
 * é o comportamento mais seguro quando o estado real é incerto.
 */
export const useFeatureFlag = (name: string): { enabled: boolean; isLoading: boolean } => {
  const [enabled, setEnabled] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;

    const check = async () => {
      try {
        const flags = await featureFlagsService.getPublicFlags();
        const flag = flags.find((f) => f.name === name);
        if (active) setEnabled(flag ? flag.enabled : false);
      } catch (error) {
        console.error(`Erro ao carregar a feature flag ${name}:`, error);
        if (active) setEnabled(false);
      } finally {
        if (active) setIsLoading(false);
      }
    };

    check();
    return () => {
      active = false;
    };
  }, [name]);

  return { enabled, isLoading };
};
