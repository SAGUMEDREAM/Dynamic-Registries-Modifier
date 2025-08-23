package cc.thonly.registry_modifier.api;

import cc.thonly.registry_modifier.mixin.NamedAccessor;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;


@SuppressWarnings({"unchecked"})
public interface DynamicRegistryManagerCallback {
    class Table {
        public static final Map<RegistryKey<? extends Registry<?>>, MutableRegistry<?>> CACHED2IMPL = new Object2ObjectLinkedOpenHashMap<>();

        public static void load(List<RegistryLoader.Loader<?>> registriesList) {
            for (RegistryLoader.Loader<?> loader : registriesList) {
                MutableRegistry<?> mutableRegistry = loader.registry();
                CACHED2IMPL.put(mutableRegistry.getKey(), mutableRegistry);
            }
        }
    }

    @Getter
    class EntryWrapper {
        final Map<RegistryKey<? extends Registry<?>>, MutableRegistry<?>> wrapper;

        EntryWrapper() {
            this.wrapper = this.createWrapper();
        }

        public static EntryWrapper create() {
            return new EntryWrapper();
        }

        Map<RegistryKey<? extends Registry<?>>, MutableRegistry<?>> createWrapper() {
            Map<RegistryKey<? extends Registry<?>>, MutableRegistry<?>> wrapper = new Object2ObjectOpenHashMap<>();
            Set<RegistryKey<? extends Registry<?>>> keys = new ObjectOpenHashSet<>();
            keys.addAll(Registries.ROOT.getKeys());
            keys.addAll(Table.CACHED2IMPL.keySet());
            keys.addAll(KEY2BUILDER.keySet());
            for (RegistryKey<? extends Registry<?>> key : keys) {
                DeferredRegister<?> deferredRegister = new DeferredRegister<>(key, Lifecycle.stable());
                puts(Registries.ROOT.get((RegistryKey<MutableRegistry<?>>) key), deferredRegister);
                puts(Table.CACHED2IMPL.get(key), deferredRegister);
                if (KEY2BUILDER.containsKey(key)) {
                    puts(KEY2BUILDER.get(key).getDeferRegistry(), deferredRegister);
                    puts(KEY2BUILDER.get(key).getInitializerRegistry(), deferredRegister);
                }
                wrapper.put(key, deferredRegister);
            }
            return wrapper;
        }

        @SuppressWarnings("rawtypes")
        void puts(MutableRegistry<?> source, SimpleRegistry target) {
            if (source == null) {
                return;
            }
            for (Map.Entry<? extends RegistryKey<?>, ?> entry : source.getEntrySet()) {
                RegistryKey<?> key = entry.getKey();
                Object value = entry.getValue();
                if (target.get(key) == null) {
                    continue;
                }
                target.add(key, value, RegistryEntryInfo.DEFAULT);
            }
            if (source instanceof SimpleRegistry ss) {
                Set<Map.Entry<TagKey, RegistryEntryList.Named>> set = ss.tags.entrySet();
                for (Map.Entry<TagKey, RegistryEntryList.Named> mapEntry : set) {
                    RegistryEntryList.Named named = mapEntry.getValue();
                    RegistryEntryOwner owner = named.owner;
                    TagKey tagKey = named.tag;
                    List<? extends RegistryEntry<?>> entries = named.entries;
                    if (owner != null && tagKey != null && entries != null) {
                        Map<TagKey, RegistryEntryList.Named> tags = target.tags;
                        if (!tags.containsKey(tagKey)) {
                            tags.put(tagKey, NamedAccessor.callNew(owner, tagKey));
                        }
                        RegistryEntryList.Named namedList = tags.get(tagKey);
                        if (namedList.entries == null) {
                            namedList.entries = new ArrayList();
                        }
                        namedList.entries.addAll(entries);
                    }
                }
            }
        }
    }

    Map<RegistryKey<? extends Registry<?>>, Builder<?>> KEY2BUILDER = new Object2ObjectOpenHashMap<>();

    static void start(SimpleRegistry<?> registry) {
        for (Builder<?> factory : KEY2BUILDER.values()) {
            if (factory.registryKey.equals(registry.getKey())) {
                factory.startBuild(registry);
            }
        }

    }

    static <T> Builder<T> createBuilder(RegistryKey<? extends Registry<T>> registryKey) {
        return (Builder<T>) KEY2BUILDER.computeIfAbsent(registryKey, key -> new Builder<>(registryKey));
    }

    class DeferredRegister<T> extends SimpleRegistry<T> {

        public DeferredRegister(RegistryKey key, Lifecycle lifecycle) {
            super(key, lifecycle);
        }

        public DeferredRegister(RegistryKey key, Lifecycle lifecycle, boolean intrusive) {
            super(key, lifecycle, intrusive);
        }
    }

    @Getter
    @Slf4j
    class Builder<T> {
        private final RegistryKey<? extends Registry<T>> registryKey;
        private final DeferredRegister<T> deferRegistry;
        private DeferredRegister<T> initializerRegistry;
        private final List<RegistrableInitializer<T>> initializers = new ArrayList<>();

        protected Builder(RegistryKey<? extends Registry<T>> registryKey) {
            assert registryKey != null;
            this.registryKey = registryKey;
            this.deferRegistry = new DeferredRegister<>(registryKey, Lifecycle.stable());
            this.initializerRegistry = new DeferredRegister<>(registryKey, Lifecycle.stable());
        }

        public T register(Identifier key, T value) {
            return register(key, value, RegistryEntryInfo.DEFAULT);
        }

        public T register(Identifier key, T value, RegistryEntryInfo info) {
            return register(key, RegistryEntry.of(value), info);
        }

        public T register(RegistryKey<T> key, T value) {
            return register(key, value, RegistryEntryInfo.DEFAULT);
        }

        public T register(RegistryKey<T> key, T value, RegistryEntryInfo info) {
            return register(key.getValue(), RegistryEntry.of(value), info);
        }

        public T register(Identifier key, RegistryEntry<T> value) {
            return register(key, value, RegistryEntryInfo.DEFAULT);
        }

        public T register(Identifier key, RegistryEntry<T> registryEntry, RegistryEntryInfo info) {
            RegistryKey<T> registryKey = RegistryKey.of(this.registryKey, key);
            T value = registryEntry.value();
            this.deferRegistry.add(registryKey, value, info);
            return value;
        }

        public void put(RegistrableInitializer<T> initializer) {
            this.initializers.add(initializer);
        }

        public void putTag(TagKey<T> tagKey, Consumer<List<T>> collector) {
            List<T> temp = new ArrayList<>();
            collector.accept(temp);
            RegistryEntryList.Named<T> registryEntries = this.deferRegistry.tags.computeIfAbsent(tagKey, key -> NamedAccessor.callNew(this.deferRegistry, tagKey));
            if (registryEntries.entries == null) {
                registryEntries.entries = new ArrayList<>();
            }
            for (T value : temp) {
                RegistryEntry<T> entry = this.deferRegistry.getEntry(value);
                if (registryEntries.entries.contains(entry)) continue;
                registryEntries.entries.add(entry);
            }
        }

        public synchronized void startBuild(SimpleRegistry<?> targetRegistry) {
            SimpleRegistry<T> target = (SimpleRegistry<T>) targetRegistry;
            this.build(this.deferRegistry, target);
            this.initializerRegistry = new DeferredRegister<>(this.registryKey, Lifecycle.stable());
            for (RegistrableInitializer<T> initializer : this.initializers) {
                initializer.bootstrap(new Registerable<>() {
                    @Override
                    public RegistryEntry.Reference<T> register(RegistryKey<T> key, T value, Lifecycle lifecycle) {
                        return initializerRegistry.add(key, value, RegistryEntryInfo.DEFAULT);
                    }

                    @SuppressWarnings("rawtypes")
                    @Override
                    public <S> RegistryEntryLookup<S> getRegistryLookup(RegistryKey<? extends Registry<? extends S>> registryRef) {
                        return new RegistryEntryLookup<>() {
                            final EntryWrapper entryWrapper = EntryWrapper.create();

                            @Override
                            public Optional<RegistryEntry.Reference<S>> getOptional(RegistryKey<S> key) {
                                Map<RegistryKey<? extends Registry<?>>, MutableRegistry<?>> wrapper = this.entryWrapper.getWrapper();
                                MutableRegistry mutableRegistry = wrapper.get(key.getRegistryRef());
                                if (mutableRegistry == null) {
                                    return Optional.empty();
                                }
                                return mutableRegistry.getOptional(key);
                            }

                            @Override
                            public Optional<RegistryEntryList.Named<S>> getOptional(TagKey<S> tag) {
                                Map<RegistryKey<? extends Registry<?>>, MutableRegistry<?>> wrapper = this.entryWrapper.getWrapper();
                                MutableRegistry mutableRegistry = wrapper.get(tag.registryRef());
                                if (mutableRegistry == null) {
                                    return Optional.empty();
                                }
                                return mutableRegistry.getOptional(tag);
                            }
                        };
                    }
                });
            }
            this.build(this.initializerRegistry, target);
            this.buildTags(this.deferRegistry, target);
        }

        protected synchronized void build(SimpleRegistry<T> source, SimpleRegistry<T> target) {
            for (Map.Entry<Identifier, RegistryEntry.Reference<T>> entry : source.idToEntry.entrySet()) {
                Identifier key = entry.getKey();
                RegistryKey<T> registryKey = RegistryKey.of(source.getKey(), key);
                T value = entry.getValue().value();
                try {
                    if (!target.contains(registryKey)
                            && !target.containsId(key)
                            && !target.idToEntry.containsKey(key)
                            && !target.valueToEntry.containsKey(value)
                    ) {
                        target.add(registryKey, value, RegistryEntryInfo.DEFAULT);
                    }
                } catch (Exception err) {
                    log.error("Can't add value for registry {}", registryKey);
                }
            }
        }

        protected void buildTags(SimpleRegistry<T> sources, SimpleRegistry<T> target) {
            for (Map.Entry<TagKey<T>, RegistryEntryList.Named<T>> mapEntry : sources.tags.entrySet()) {
                TagKey<T> key = mapEntry.getKey();
                RegistryEntryList.Named<T> value = mapEntry.getValue();
                RegistryEntryList.Named<T> named = target.tags.computeIfAbsent(key, x -> NamedAccessor.callNew(target, key));
                if (named.entries == null) {
                    named.entries = new ArrayList<>();
                }
                if (isImmutableList(named.entries)) {
                    named.entries = new ArrayList<>(named.entries);
                }
                if (value.entries != null) {
                    named.entries.addAll(value.entries);
                }

            }
        }

        private boolean isImmutableList(List<?> list) {
            if (list instanceof com.google.common.collect.ImmutableList) {
                return true;
            }
            try {
                list.add(null);
                list.remove(null);
                return false;
            } catch (UnsupportedOperationException e) {
                return true;
            }
        }

        public interface RegistrableInitializer<T> {
            void bootstrap(Registerable<T> context);
        }

    }
}
