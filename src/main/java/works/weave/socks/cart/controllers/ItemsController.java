package works.weave.socks.cart.controllers;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
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
import works.weave.socks.cart.entities.Item;
import works.weave.socks.cart.item.FoundItem;
import works.weave.socks.cart.item.ItemDAO;
import works.weave.socks.cart.item.ItemResource;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;


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

    private ArrayList<Integer> requestsArray = new ArrayList<String>();
    private int requestTrimThreshold = 5000;
    private int requestTrimSize = 4000;

    public static final String FAULTY_ITEM_ID = "03fef6ac-1896-4ce8-bd69-b798f85c6e0f";
    public static final Integer MAX_JOBCOUNT = 2;

    static final Counter requests = Counter.build().name("requests_total").help("Total number of requests.").register();
    static final Histogram requestLatency = Histogram.build().name("requests_latency_seconds")
            .help("Request latency in seconds.").register();


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

    private int getRequestsPerMinute() {
        Calendar calendar = Calendar.getInstance();
        int now = calendar.get(Calendar.MILLISECOND);
        int aMinuteAgo = now - (1000 * 60);
        int cnt = 0;
        // since recent requests are at the end of the array, search the array
        // from back to front
        for (int i = this.requestsArray.size() - 1; i >= 0; i--) {
            if (requestsArray[i] >= aMinuteAgo) {
                ++cnt;
            } else {
                break;
            }
        }
        return cnt;
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
        return cartsController.get(customerId).contents();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public Item addToCart(@PathVariable String customerId, @RequestBody Item item) throws Exception {
        Histogram.Timer requestTimer = null;

        if (prometheusEnabled.equals("true")) {
            requests.inc();
            requestTimer = requestLatency.startTimer();
        }

        Calendar calendar = Calendar.getInstance();
        int now = calendar.get(Calendar.MILLISECOND);

        this.requestsArrayrequestsArray.add(now);

        System.out.println("Number of requests per minute: " + this.getRequestsPerMinute());

        // now keep requests array from growing forever
        if (this.requestsArray.size() > this.requestTrimThreshold) {
            this.requestsArray = this.requestsArray.subList(0, requests.size() - this.requestTrimSize);
        }

        try {
            try {
                int millis = Integer.parseInt(this.delayInMillis.trim());
                Thread.sleep(millis);
            } catch (Throwable e) {
                // don't do anything
            }

            int promRate = Integer.parseInt(promotionRate);
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
                System.out.println("found item id: " + newItem.getItemId());
                if (newItem.getItemId().equals(FAULTY_ITEM_ID)) {
                    System.out.println("special item found - do some calculation to increase CPU load");
                    int jobCount = 0;
                    while (jobCount < MAX_JOBCOUNT) {
                        long count = 0;
                        long max = 0;
                        for (long i = 3; i <= 23000; i++) {
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
