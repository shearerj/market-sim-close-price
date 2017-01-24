package edu.umich.srg.marketsim;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;

import org.reflections.Reflections;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

final class EntityBuilder {

  private static final String pack = EntityBuilder.class.getPackage().getName();
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static List<Function<String, String>> nameTweaks =
      Arrays.asList(String::toLowerCase, s -> s.replaceAll("[^A-Z]+", "").toLowerCase());

  public interface AgentCreator {
    Agent createAgent(Sim sim, Fundamental fundamental, Collection<Market> markets, Market market,
        Spec spec, Random rand);
  }

  public interface MarketCreator {
    Market createMarket(Sim sim, Fundamental fundamental, Spec spec, Random rand);
  }

  static AgentCreator getAgentCreator(String name) {
    return checkNotNull(agentNameMap.get(name),
        "\"%s\" is not a defined agent name in EntityBuilder", name).getValue();
  }

  static MarketCreator getMarketCreator(String name) {
    return checkNotNull(marketNameMap.get(name),
        "\"%s\" is not a defined market name in EntityBuilder", name).getValue();
  }

  private static final Map<String, Entry<Class<?>, AgentCreator>> agentNameMap =
      classCreatorMap(AgentCreator.class, "createFromSpec", nameTweaks);

  private static final Map<String, Entry<Class<?>, MarketCreator>> marketNameMap =
      classCreatorMap(MarketCreator.class, "createFromSpec", nameTweaks);

  /**
   * If a bunch of classes follow a standard static interface, this method can be used to generate
   * human readable strings that map to those interfaces. This function will find all non-abstract
   * classes that are subclasses of the return type of `functionClass`, and have a public static
   * method named `interfaceName` that implements that interface but returns the class they're
   * defined in (e.g. factory constructors). For each method that fits these criteria, this will
   * insert them into a map with human strings mapping to the functions. By default, the strings are
   * the simpleClassNames less a suffix equal to the functional interface return type class. Every
   * function in `nameTweaks` will be applied to those strings, and any unique strings will be
   * preserved in the final map.
   * 
   * @param functionClass the functional interface
   * @param interfaceName the name of the public static function implementing the functional
   *        interface for all classes
   * @param nameTweaks a collection of string to string mappings to generate names
   */
  private static <F> Map<String, Entry<Class<?>, F>> classCreatorMap(Class<F> functionClass,
      String interfaceName, Collection<Function<String, String>> nameTweaks) {
    Method[] methods = functionClass.getMethods();
    assert methods.length == 1 : "functionClass is not a functional interface";
    Method method = methods[0];
    Class<?> base = method.getReturnType();
    MethodType interfaceType = MethodType.methodType(base, method.getParameterTypes());
    String suffix = base.getSimpleName() + "$";

    Map<String, Map<Class<?>, F>> dupMap = new HashMap<>();

    for (Class<?> cls : new Reflections(pack).getSubTypesOf(base)) {
      if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {
        continue;
      }

      String name = cls.getSimpleName().replaceFirst(suffix, "");
      MethodType methodType = interfaceType.changeReturnType(cls);
      F func;
      try {
        MethodHandle handle = LOOKUP.findStatic(cls, interfaceName, methodType);
        func = MethodHandleProxies.asInterfaceInstance(functionClass, handle);
      } catch (NoSuchMethodException e) {
        System.err.format("Could not find static method `%s` with signature %s in %s\n",
            interfaceName, methodType, cls.getName());
        continue;
      } catch (IllegalAccessException e) {
        System.err.format("Could not access static method `%s` in %s\n", interfaceName,
            cls.getName());
        continue;
      }

      for (Function<String, String> f : nameTweaks) {
        dupMap.computeIfAbsent(f.apply(name), k -> new HashMap<>()).put(cls, func);
      }
    }

    return dupMap.entrySet().stream().filter(e -> e.getValue().size() == 1).collect(
        Collectors.toMap(Entry::getKey, e -> Iterables.getOnlyElement(e.getValue().entrySet())));
  }

  private static void printMap(Map<String, ? extends Entry<Class<?>, ?>> map, Writer writer) {
    map.entrySet().stream().sorted((fst, snd) -> fst.getKey().compareTo(snd.getKey()))
        .forEachOrdered(ent -> {
          try {
            writer.write(ent.getKey());
            writer.write(" : ");
            writer.write(ent.getValue().getKey().getSimpleName());
            writer.write('\n');
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }

  static void printAgents(Writer writer) {
    printMap(agentNameMap, writer);
  }

  static void printMarkets(Writer writer) {
    printMap(marketNameMap, writer);
  }

}
