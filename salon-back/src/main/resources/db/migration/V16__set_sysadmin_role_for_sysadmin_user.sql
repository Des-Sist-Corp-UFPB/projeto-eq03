-- Fix role for user sysadmin@salao.com to SYSADMIN
UPDATE tb_user
SET role_id = (SELECT id FROM tb_role WHERE name = 'SYSADMIN')
WHERE email = 'sysadmin@salao.com';
