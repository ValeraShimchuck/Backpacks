package ua.valeriishymchuk.backpacks.repository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import ua.valeriishymchuk.backpacks.common.configuration.ConfigLoader;
import ua.valeriishymchuk.backpacks.entities.ConfigEntity;
import ua.valeriishymchuk.backpacks.entities.LangEntity;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ConfigRepository implements IConfigRepository {

    ConfigLoader configLoader;
    @NonFinal
    private ConfigEntity config;
    @NonFinal
    private LangEntity lang;

    @Override
    public void reload() {
        config = configLoader.loadOrSave(ConfigEntity.class, "config");
        lang = configLoader.loadOrSave(LangEntity.class, "lang");
    }

    @Override
    public LangEntity getLang() {
        return lang;
    }

    @Override
    public ConfigEntity getConfig() {
        return config;
    }
}
