
    create table ICLICKER_LOCK (
        ID number(19,0) not null,
        LAST_MODIFIED timestamp not null,
        NAME varchar2(255 char) not null unique,
        HOLDER varchar2(255 char) not null,
        primary key (ID)
    );

    create table ICLICKER_REGISTRATION (
        id number(19,0) not null,
        clickerId varchar2(16 char) not null,
        ownerId varchar2(255 char) not null,
        locationId varchar2(255 char),
        activated number(1,0) not null,
        national number(1,0) not null,
        dateCreated timestamp not null,
        dateModified timestamp not null,
        primary key (id)
    );

    create table ICLICKER_USER_KEY (
        ID number(19,0) not null,
        USER_ID varchar2(255 char) not null unique,
        USER_KEY varchar2(255 char) not null,
        dateCreated timestamp not null,
        dateModified timestamp not null,
        primary key (ID)
    );

    create index clicker_lock_name on ICLICKER_LOCK (NAME);

    create index clicker_user_key on ICLICKER_USER_KEY (USER_KEY);

    create index clicker_user_id on ICLICKER_USER_KEY (USER_ID);

    create sequence ICLICKER_REG_ID_SEQ;

    create sequence hibernate_sequence;
