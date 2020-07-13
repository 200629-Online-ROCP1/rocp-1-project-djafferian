-- DROP SEQUENCE banking.users_seq;

CREATE SEQUENCE banking.users_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;