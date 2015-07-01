# --- !Ups

create table "elements" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"path" VARCHAR(254) NOT NULL,"category" INTEGER NOT NULL,"explicit" BOOLEAN NOT NULL,"level" INTEGER NOT NULL,"inchallenge" BOOLEAN NOT NULL,"user_id" BIGINT NOT NULL);
create unique index "IDX_PATH" on "elements" ("path");
create table "users" ("user_id" BIGSERIAL NOT NULL PRIMARY KEY,"first_name" VARCHAR(254) NOT NULL,"last_name" VARCHAR(254) NOT NULL,"email" VARCHAR(254) NOT NULL,"avatar_url" VARCHAR(254));
create unique index "IDX_EMAIL" on "users" ("email");
create table "users_logininfo" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"provider_id" VARCHAR(254) NOT NULL,"provider_key" VARCHAR(254) NOT NULL);
create table "users_passwordinfo" ("hasher" VARCHAR(254) NOT NULL,"password" VARCHAR(254) NOT NULL,"salt" VARCHAR(254),"logininfo_id" BIGINT NOT NULL PRIMARY KEY);
create table "users_userlogininfo" ("user_id" BIGINT NOT NULL,"logininfo_id" BIGINT NOT NULL);

# --- !Downs

drop table "users_userlogininfo";
drop table "users_passwordinfo";
drop table "users_logininfo";
drop table "users";
drop table "elements";
