
    create table ICLICKER_LOCK (
        ID bigint not null auto_increment,
        LAST_MODIFIED datetime not null,
        NAME varchar(255) not null unique,
        HOLDER varchar(255) not null,
        primary key (ID)
    ) ENGINE=InnoDB;

    create table ICLICKER_REGISTRATION (
        id bigint not null auto_increment,
        clickerId varchar(16) not null,
        ownerId varchar(255) not null,
        locationId varchar(255),
        activated bit not null,
        national bit not null,
        dateCreated datetime not null,
        dateModified datetime not null,
        primary key (id)
    ) ENGINE=InnoDB;

    create table ICLICKER_USER_KEY (
        ID bigint not null auto_increment,
        USER_ID varchar(255) not null unique,
        USER_KEY varchar(255) not null,
        dateCreated datetime not null,
        dateModified datetime not null,
        primary key (ID)
    ) ENGINE=InnoDB;

    create index clicker_lock_name on ICLICKER_LOCK (NAME);

    create index clicker_user_key on ICLICKER_USER_KEY (USER_KEY);

    create index clicker_user_id on ICLICKER_USER_KEY (USER_ID);
