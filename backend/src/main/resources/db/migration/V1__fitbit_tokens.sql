create table if not exists fitbit_token (
                                            id bigserial primary key,
                                            fitbit_user_id varchar(64) not null,
    access_token text not null,
    refresh_token text not null,
    scope text,
    token_type varchar(32),
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (fitbit_user_id)
    );

create index if not exists idx_fitbit_token_user on fitbit_token(fitbit_user_id);