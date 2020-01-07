package works.weave.socks.cart.controllers;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import works.weave.socks.cart.cart.CartDAO;
import works.weave.socks.cart.cart.CartResource;
import works.weave.socks.cart.entities.Cart;
import works.weave.socks.cart.entities.Item;
import works.weave.socks.cart.item.FoundItem;
import works.weave.socks.cart.item.ItemDAO;
import works.weave.socks.cart.item.ItemResource;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

import no.finn.unleash.Unleash;
import no.finn.unleash.DefaultUnleash;
import no.finn.unleash.util.UnleashConfig;

@RestController
@RequestMapping(value = "/carts/{customerId:.*}/items")
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
public class ItemsController {
    private final Logger LOG = getLogger(getClass());

    @Autowired
    private ItemDAO itemDAO;
    @Autowired
    private CartsController cartsController;
    @Autowired
    private CartDAO cartDAO;
    @Value("${delayInMillis}")
    private String delayInMillis;
    @Value("0")
    private String promotionRate;
    @Value("${endpoints.prometheus.enabled}")
    private String prometheusEnabled;

    public static final String FAULTY_ITEM_ID   = "03fef6ac-1896-4ce8-bd69-b798f85c6e0f";
    public static final String SLOW_ITEM_ID     = "03fef6ac-1896-4ce8-bd69-b798f86c6fac";
    public static final Integer SLOW_ITEM_SLEEP = 2000;
    public static final Integer MAX_JOBCOUNT    = 2;

    static final Counter requests = Counter.build().name("requests_total").help("Total number of requests.").register();
    static final Histogram requestLatency = Histogram.build().name("requests_latency_seconds")
            .help("Request latency in seconds.").register();

    static final UnleashConfig unleashConfig = null;
    static Unleash unleash = null;

    public Unleash getUnleash() {
        if (unleash == null && System.getenv("UNLEASH_SERVER_URL")) {
            unleashConfig = UnleashConfig.builder()
                    .appName("Carts")
                    .instanceId("instance x")
                    .unleashAPI(System.getenv("UNLEASH_SERVER_URL"))
                    .build();
            unleash = new DefaultUnleash(unleashConfig);
        }
        return unleash;
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/{itemId:.*}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public Item get(@PathVariable String customerId, @PathVariable String itemId) {
        return new FoundItem(() -> getItems(customerId), () -> new Item(itemId)).get();
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/delay/{delay}", method = RequestMethod.GET)
    public void setDelayInMillis(@PathVariable("delay") Optional<String> delayInMillis) {
        String newDelay = "0";

        if (delayInMillis.isPresent()) {
            newDelay = delayInMillis.get();
        }

        this.delayInMillis = newDelay;
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/promotion/{promotion_rate}", method = RequestMethod.GET)
    public void setPromotionRate(@PathVariable("promotion_rate") Optional<String> promotionRate) {
        String newPromotionRate = "0";

        if (promotionRate.isPresent()) {
            newPromotionRate = promotionRate.get();
        }

        this.promotionRate = newPromotionRate;
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public List<Item> getItems(@PathVariable String customerId) {
        try {
            int millis = Integer.parseInt(this.delayInMillis.trim());
            Thread.sleep(millis);
        } catch (Throwable e) {
            // don't do anything
        }

        if(getUnleash().isEnabled("EnableItemCache")) {
            LOG.debug("using carts cache");
            // return cached items
            Cart cart = new Cart();
            cart.customerId = customerId;
            String id = "5d9e1acc5bd23600071eecea";
            String itemId = "03fef6ac-1896-4ce8-bd69-b798f85c6e0b";
            cart.add(new Item(id, itemId, 5, 99.99f));
            return cart.contents();
          } else {
            LOG.debug("not using carts cache");
            try {
                Thread.sleep((long)(Math.random() * 100)+100);
            } catch (Exception e) {
                //TODO: handle exception
            }
            return cartsController.get(customerId).contents();
          }


        
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public Item addToCart(@PathVariable String customerId, @RequestBody Item item) throws Exception {
        Histogram.Timer requestTimer = null;

        if (prometheusEnabled.equals("true")) {
            requests.inc();
            requestTimer = requestLatency.startTimer();
        }

        try {
            try {
                int millis = Integer.parseInt(this.delayInMillis.trim());
                Thread.sleep(millis);
            } catch (Throwable e) {
                // don't do anything
            }

            int promRate = Integer.parseInt(promotionRate);
            // overwrite if promotionrate is set via feature toggle
            if(getUnleash().isEnabled("EnablePromotion")) {
                promRate = 30;
            }
            if (promRate >= (Math.random() * 100)) {
                throw new Exception("promotion campaign not yet implemented");
            }

            // If the item does not exist in the cart, create new one in the repository.
            FoundItem foundItem = new FoundItem(() -> cartsController.get(customerId).contents(), () -> item);

            if (!foundItem.hasItem()) {
                Supplier<Item> newItem = new ItemResource(itemDAO, () -> item).create();
                LOG.debug("Did not find item. Creating item for user: " + customerId + ", " + newItem.get());
                new CartResource(cartDAO, customerId).contents().get().add(newItem).run();
                return item;
            } else {
                Item newItem = new Item(foundItem.get(), foundItem.get().quantity() + 1);
                //System.out.println("found item id: " + newItem.getItemId());
                if (newItem.getItemId().equals(FAULTY_ITEM_ID)) {
                    System.out.println("special item found - do some calculation to increase CPU load");
                    int jobCount = 0;
                    while (jobCount < MAX_JOBCOUNT) {
                        long count = 0;
                        long max = 0;
                        for (long i = 3; i <= 10000; i++) {
                            boolean isPrime = true;
                            for (long j = 2; j <= i / 2 && isPrime; j++) {
                                isPrime = i % j > 0;
                            }
                            if (isPrime) {
                                count++;
                                max = i;
                                System.out.println("prime: " + i);
                            }
                        }
                        jobCount++;
                    }
                }
                else if (newItem.getItemId().equals(SLOW_ITEM_ID)) {
                    try {
                        Thread.sleep(SLOW_ITEM_SLEEP);
                    } catch (Throwable e) {
                        // don't do anything
                    }
                }
                LOG.debug("Found item in cart. Incrementing for user: " + customerId + ", " + newItem);
                updateItem(customerId, newItem);
                return newItem;
            }
        } finally {
            if (prometheusEnabled.equals("true") && requestTimer != null) {
                requestTimer.observeDuration();
            }
        }
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequestMapping(value = "/{itemId:.*}", method = RequestMethod.DELETE)
    public void removeItem(@PathVariable String customerId, @PathVariable String itemId) {
        FoundItem foundItem = new FoundItem(() -> getItems(customerId), () -> new Item(itemId));
        Item item = foundItem.get();

        LOG.debug("Removing item from cart: " + item);
        new CartResource(cartDAO, customerId).contents().get().delete(() -> item).run();

        LOG.debug("Removing item from repository: " + item);
        new ItemResource(itemDAO, () -> item).destroy().run();
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PATCH)
    public void updateItem(@PathVariable String customerId, @RequestBody Item item) {
        // Merge old and new items
        ItemResource itemResource = new ItemResource(itemDAO, () -> get(customerId, item.itemId()));
        LOG.debug("Merging item in cart for user: " + customerId + ", " + item);
        itemResource.merge(item).run();
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(method = RequestMethod.GET, path = "/health")
    public @ResponseBody String getHealth() {
        return "OK - endpoint available";
    }

    // @ResponseStatus(HttpStatus.OK)
    // @RequestMapping(method = RequestMethod.GET, path = "/memoryLeak/{loops}")
    // public void createMemoryLeak(@PathVariable("loops") Optional<String>
    // loopNumber) {
    // class BadKey {
    // // no hashCode or equals();
    // public final String key;

    // public BadKey(String key) {
    // this.key = key;
    // }
    // }
    // Map map = System.getProperties();

    // int counter = 0;
    // if (loopNumber.isPresent()) {
    // int loops = Integer.parseInt(loopNumber.get());
    // while (counter < loops) {
    // map.put(new BadKey("key"), new String("value"));
    // counter++;
    // }
    // return;
    // }
    // }

}
