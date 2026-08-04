DELETE FROM tb_cashflow cf
WHERE cf.appointment_id IS NOT NULL
  AND cf.id NOT IN (
      SELECT MIN(cf2.id)
      FROM tb_cashflow cf2
      WHERE cf2.appointment_id = cf.appointment_id
      GROUP BY cf2.appointment_id
  );
