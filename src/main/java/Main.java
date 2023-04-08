import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Main {
  public static Map<Integer, Integer> sizeToFreq = new HashMap<>();

  public static void main(String[] args) {
    String[] routs = new String[1000];
    String letterToSearch = "R";

    AtomicInteger currentMaxSizeKey = new AtomicInteger();
    AtomicInteger currentMaxSizeValue = new AtomicInteger();

    for (int i = 0; i < routs.length; i++) {
      routs[i] = generateRoute("RLRFR", 100);
    }

    for (String rout : routs) {
      Runnable processingLogic = () -> {
        AtomicInteger maxSize = new AtomicInteger();

        rout.chars()
          .mapToObj(c -> (char) c)
          .forEach(character -> {
            if (character.toString().equals(letterToSearch)) {
              maxSize.getAndIncrement();
            }
          });

        synchronized (sizeToFreq) {
          if (sizeToFreq.containsKey(maxSize.get())) {
            sizeToFreq.put(maxSize.get(), sizeToFreq.get(maxSize.get())+1);
          } else {
            sizeToFreq.put(maxSize.get(), 1);
          }
        }

        synchronized (currentMaxSizeKey) {
          if (currentMaxSizeValue.get() < sizeToFreq.get(maxSize.get())) {
            currentMaxSizeValue.set(sizeToFreq.get(maxSize.get()));
            currentMaxSizeKey.set(maxSize.get());
            currentMaxSizeKey.notify();
          }
        }
      };

      Runnable searchLargestLogic = () -> {
          synchronized (currentMaxSizeKey) {
          try {
            currentMaxSizeKey.wait();
            System.out.println("Текущий лидер: " + currentMaxSizeKey);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      };

      Thread processingThread = new Thread(processingLogic);
      Thread searchLargestThread = new Thread(searchLargestLogic);
      processingThread.start();
      searchLargestThread.start();
    }

    sizeToFreq = sizeToFreq.entrySet().stream()
      .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

    renderResults(sizeToFreq);
  }

  public static void renderResults(Map<Integer, Integer> sizeToFreq) {
    int index = 0;
    for (Integer key : sizeToFreq.keySet()) {
      if (index == 0) {
        System.out.printf(
          """
            Самое частое количество повторений %d (встретилось %d раз)
            Другие размеры:\s
            """, key, sizeToFreq.get(key));
      } else {
        System.out.printf("- %d (%d раз)\n", key, sizeToFreq.get(key));
      }
      index++;
    }
  }


  public static String generateRoute(String letters, int length) {
    Random random = new Random();
    StringBuilder route = new StringBuilder();
    for (int i = 0; i < length; i++) {
      route.append(letters.charAt(random.nextInt(letters.length())));
    }
    return route.toString();
  }
}
