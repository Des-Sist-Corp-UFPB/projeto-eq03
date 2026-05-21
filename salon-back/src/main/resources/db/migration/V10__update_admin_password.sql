-- Update password for user admin@salao.com (default ADMIN)
UPDATE tb_user
SET password = '$2b$10$muQE02G9kABGDwhyB5Ef.eXa9n5iZGKPvFnUjVxcBm7ot.JqxYcQK'
WHERE email = 'admin@salao.com';

-- Update password for user sysadmin@salao.com (default SYSADMIN)
UPDATE tb_user
SET password = '$2b$10$dPM0.QrVOO3.1zXuZamFweNAkAZbnZIszjnKzRKBJTAI85oBSLyfe'
WHERE email = 'sysadmin@salao.com';
