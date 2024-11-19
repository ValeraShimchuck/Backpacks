package ua.valeriishymchuk.backpacks.repository;

import ua.valeriishymchuk.backpacks.entities.ConfigEntity;
import ua.valeriishymchuk.backpacks.entities.LangEntity;

public interface IConfigRepository {

    void reload();

    LangEntity getLang();
    ConfigEntity getConfig();

}
