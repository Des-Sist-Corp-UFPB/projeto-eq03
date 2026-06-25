-- Update password for user admin@salao.com (default ADMIN)
UPDATE tb_user
SET password = '$2a$10$Mjdb/UO5pKA1u8yrVyc7Ueu8iYf.7bMdIdeTg/vCbTJ9vaMHnaWdW'
WHERE email = 'admin@salao.com';

-- Update password for user sysadmin@salao.com (default SYSADMIN)
UPDATE tb_user
SET password = '$2a$10$uqM0RUcT292MM9uuHly9CuD0j5trnUUSd93/UoZC.3Ix3Ty//Q5hO'
WHERE email = 'sysadmin@salao.com';
