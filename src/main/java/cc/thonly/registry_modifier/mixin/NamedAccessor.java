package cc.thonly.registry_modifier.mixin;

import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RegistryEntryList.Named.class)
public interface NamedAccessor<T> {
    @Accessor("owner")
    RegistryEntryOwner<T> getOwner();

    @Accessor("tag")
    TagKey<T> getTagKey();

    @Invoker("<init>")
    static <T> RegistryEntryList.Named<T> callNew(RegistryEntryOwner<T> owner, TagKey<T> tag) {
        throw new AssertionError();
    }
}
