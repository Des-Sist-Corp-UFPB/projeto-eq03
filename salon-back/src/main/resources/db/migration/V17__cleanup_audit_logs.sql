DELETE FROM tb_audit_log WHERE action LIKE 'GET%' OR action = 'PING' OR action IS NULL;
