package ua.valeriishymchuk.backpacks.common.component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Collection;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigSerializable
public class RawComponent {

    @Getter
    @Setting(nodeFromParent = true)
    List<String> raw;

    public RawComponent(List<String> raw) {
        this.raw = raw;
    }

    public RawComponent(String... raw) {
        this(List.of(raw));
    }

    private RawComponent() {
        this(List.of());
    }

    public Component bake() {
        Component message = Component.empty();
        for (int i = 0; i < raw.size(); i++) {
            String line = raw.get(i);
            message = message.append(Component.text(line));
            if (i + 1 < raw.size()) {
                message = message.append(Component.newline());
            }
        }
        return message;
    }

    public List<Component> bakeAsLore() {
        return raw.stream().map(MiniMessage.miniMessage()::deserialize).toList();
    }

    public RawComponent replaceText(String placeholder, String text) {
        return new RawComponent(
                raw.stream().map(s -> s.replace(placeholder, text)).toList()
        );
    }

    public RawComponent replaceText(String placeholder, Component text) {
        return replaceText(placeholder, MiniMessage.miniMessage().serialize(text));
    }

    public RawComponent replaceText(String placeholder, RawComponent text) {
        return replaceText(placeholder, text.bake());
    }

    public RawComponent replaceText(String placeholder, Object obj) {
        return replaceText(placeholder, obj.toString());
    }

    public void send(Audience audience) {
        audience.sendMessage(bake());
    }

    public void send(Collection<? extends Audience> audiences) {
        audiences.forEach(this::send);
    }

    public void send() {
        send(Bukkit.getOnlinePlayers());
        send(Bukkit.getConsoleSender());
    }


}
