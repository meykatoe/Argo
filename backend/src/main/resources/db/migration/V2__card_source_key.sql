alter table card add column source_key varchar(200);

update card set source_key = set_id || '|' || card_image_id || '|' || id;

alter table card alter column source_key set not null;
alter table card add constraint uq_card_source_key unique (source_key);
