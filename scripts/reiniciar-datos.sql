-- Deja el esquema GESTION vacío para que Flyway vuelva a crear todo (V1..Vn) al siguiente arranque.
-- Sirve para ensayar la demostración varias veces partiendo del mismo caso de prueba.
--   docker exec -i gestion-pro-oracle sqlplus -s gestion/gestion@FREEPDB1 < scripts/reiniciar-datos.sql
-- Detén el backend antes de ejecutarlo y vuelve a iniciarlo después.
SET SERVEROUTPUT ON
BEGIN
  FOR o IN (SELECT object_name, object_type FROM user_objects
             WHERE object_type IN ('VIEW', 'SYNONYM', 'PROCEDURE', 'FUNCTION', 'PACKAGE', 'TRIGGER', 'SEQUENCE', 'TYPE')
               AND object_name NOT LIKE 'ISEQ$$%') LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP ' || o.object_type || ' "' || o.object_name || '"';
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
  END LOOP;
  FOR t IN (SELECT table_name FROM user_tables) LOOP
    EXECUTE IMMEDIATE 'DROP TABLE "' || t.table_name || '" CASCADE CONSTRAINTS PURGE';
  END LOOP;
  DBMS_OUTPUT.PUT_LINE('Esquema vaciado. Inicia el backend para recrear los datos de demostración.');
END;
/
EXIT
