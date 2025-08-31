package com.ddtt.controllers;

import com.ddtt.jooq.generated.tables.*;
import com.ddtt.jooq.generated.tables.records.*;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.mindrot.jbcrypt.BCrypt;
import static com.ddtt.jooq.generated.tables.Rating.RATING;
import static com.ddtt.jooq.generated.tables.BookView.BOOK_VIEW;
import static com.ddtt.jooq.generated.tables.ReadingProgress.READING_PROGRESS;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jooq.Query;

@Controller("/api")
@RequiredArgsConstructor
public class Seeder {

    private final int BATCH_SIZE = 1000;
    private final int THREADS = 8;
    private final DSLContext dsl;

    @Get("/seeder-user")
    public void seederUser() {
        int NUM_USERS = 100000;
        String defaultPasswordHash = BCrypt.hashpw("123456", BCrypt.gensalt());
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        int totalBatches = (int) Math.ceil((double) NUM_USERS / BATCH_SIZE);

        for (int b = 0; b < totalBatches; b++) {
            final int batchStart = b * BATCH_SIZE + 1;
            final int batchEnd = Math.min((b + 1) * BATCH_SIZE, NUM_USERS);

            executor.submit(() -> {
                List<AccountRecord> batch = new ArrayList<>(BATCH_SIZE);
                Random rnd = new Random();

                for (int i = batchStart; i <= batchEnd; i++) {
                    AccountRecord r = dsl.newRecord(Account.ACCOUNT);
                    r.setDisplayName("User_" + i);
                    r.setEmail("user" + i + "@example.com");
                    r.setPasswordHash(defaultPasswordHash);
                    r.setBalance(rnd.nextInt(1000));
                    r.setRole("USER");
                    r.setAvatarUrl("https://res.cloudinary.com/dddfgg9yo/image/upload/v1755609196/bl1rwq6xq5biwgawjxwn.webp");

                    batch.add(r);
                }

                dsl.batchInsert(batch).execute();
                System.out.println("Inserted batch: " + batchStart + " - " + batchEnd);
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }

        System.out.println("Seeding complete!");
    }

    private final int MAX_BOOK_ID = 51;
    private final int MIN_RATINGS = 100;
    private final int MAX_RATINGS = 40000;
    private final int NUM_USERS = 100013;
    private final double PERCENT_HOT = 0.17;
    private final double PERCENT_WARM = 0.28;
    private final double PERCENT_NEUTRAL = 0.5;
    private final double PERCENT_RARE = 0.05;
    private final double HOT_MU_MIN = 4.6, HOT_MU_MAX = 4.95, HOT_SIGMA = 0.35;
    private final double WARM_MU_MIN = 4.0, WARM_MU_MAX = 4.35, WARM_SIGMA = 0.35;
    private final double NEUTRAL_MU_MIN = 3.75, NEUTRAL_MU_MAX = 3.95, NEUTRAL_SIGMA = 0.12;
    private final double RARE_MU_MIN = 2.2, RARE_MU_MAX = 3.0, RARE_SIGMA = 0.6;

    // weight ranges control how many ratings a book gets relative to others
    private final double HOT_WEIGHT_MIN = 7.0, HOT_WEIGHT_MAX = 10.0;
    private final double WARM_WEIGHT_MIN = 3.0, WARM_WEIGHT_MAX = 6.0;
    private final double NEUTRAL_WEIGHT_MIN = 1.5, NEUTRAL_WEIGHT_MAX = 2.5;
    private final double RARE_WEIGHT_MIN = 0.5, RARE_WEIGHT_MAX = 1.0;

    // prime offset for selecting different user slices per book
    private static final int OFFSET_PRIME = 9973;

    @Get("/seeder-rating")
    public void seedRatingFinal() {
        Random globalRnd = new Random();

        // --- 1) build shuffled users array once (1..NUM_USERS) ---
        int[] shuffledUsers = new int[NUM_USERS];
        for (int i = 0; i < NUM_USERS; i++) {
            shuffledUsers[i] = i + 1;
        }
        // Fisher-Yates shuffle
        for (int i = NUM_USERS - 1; i > 0; i--) {
            int j = globalRnd.nextInt(i + 1);
            int tmp = shuffledUsers[i];
            shuffledUsers[i] = shuffledUsers[j];
            shuffledUsers[j] = tmp;
        }

        // --- 2) decide clusters and per-book mu/sigma/weight ---
        List<Integer> bookIds = new ArrayList<>();
        for (int i = 1; i <= MAX_BOOK_ID; i++) {
            bookIds.add(i);
        }
        Collections.shuffle(bookIds, globalRnd);

        int nHot = (int) Math.round(MAX_BOOK_ID * PERCENT_HOT);
        int nWarm = (int) Math.round(MAX_BOOK_ID * PERCENT_WARM);
        int nNeutral = (int) Math.round(MAX_BOOK_ID * PERCENT_NEUTRAL);
        int assigned = nHot + nWarm + nNeutral;
        int nRare = Math.max(0, MAX_BOOK_ID - assigned);

        Map<Integer, Double> bookMu = new HashMap<>();
        Map<Integer, Double> bookSigma = new HashMap<>();
        Map<Integer, Double> bookWeight = new HashMap<>();

        for (int idx = 0; idx < bookIds.size(); idx++) {
            int bookId = bookIds.get(idx);
            if (idx < nHot) {
                bookMu.put(bookId, HOT_MU_MIN + globalRnd.nextDouble() * (HOT_MU_MAX - HOT_MU_MIN));
                bookSigma.put(bookId, HOT_SIGMA);
                bookWeight.put(bookId, HOT_WEIGHT_MIN + globalRnd.nextDouble() * (HOT_WEIGHT_MAX - HOT_WEIGHT_MIN));
            } else if (idx < nHot + nWarm) {
                bookMu.put(bookId, WARM_MU_MIN + globalRnd.nextDouble() * (WARM_MU_MAX - WARM_MU_MIN));
                bookSigma.put(bookId, WARM_SIGMA);
                bookWeight.put(bookId, WARM_WEIGHT_MIN + globalRnd.nextDouble() * (WARM_WEIGHT_MAX - WARM_WEIGHT_MIN));
            } else if (idx < nHot + nWarm + nNeutral) {
                bookMu.put(bookId, NEUTRAL_MU_MIN + globalRnd.nextDouble() * (NEUTRAL_MU_MAX - NEUTRAL_MU_MIN));
                bookSigma.put(bookId, NEUTRAL_SIGMA);
                bookWeight.put(bookId, NEUTRAL_WEIGHT_MIN + globalRnd.nextDouble() * (NEUTRAL_WEIGHT_MAX - NEUTRAL_WEIGHT_MIN));
            } else {
                bookMu.put(bookId, RARE_MU_MIN + globalRnd.nextDouble() * (RARE_MU_MAX - RARE_MU_MIN));
                bookSigma.put(bookId, RARE_SIGMA);
                bookWeight.put(bookId, RARE_WEIGHT_MIN + globalRnd.nextDouble() * (RARE_WEIGHT_MAX - RARE_WEIGHT_MIN));
            }
        }

        // --- 3) compute per-book target ratings by scaling weight into MIN..MAX range ---
        double maxWeightPossible = HOT_WEIGHT_MAX;
        Map<Integer, Integer> bookRatings = new LinkedHashMap<>();
        for (int bookId : bookIds) {
            double w = bookWeight.get(bookId);
            int total = MIN_RATINGS + (int) Math.round((w / maxWeightPossible) * (MAX_RATINGS - MIN_RATINGS));
            total = Math.min(total, NUM_USERS); // can't exceed number of users
            bookRatings.put(bookId, total);
        }

        // --- 4) precompute score probabilities per book (from gaussian around mu) ---
        Map<Integer, double[]> bookScoreProbs = new HashMap<>();
        double totalWeight = 0.0;
        double weightedSumMu = 0.0;
        for (int bookId : bookIds) {
            double mu = bookMu.get(bookId);
            double sigma = bookSigma.get(bookId);
            bookScoreProbs.put(bookId, makeScoreProbabilities(mu, sigma));

            int cnt = bookRatings.get(bookId);
            weightedSumMu += mu * cnt;
            totalWeight += cnt;
        }
        double predictedAvg = weightedSumMu / totalWeight;
        System.out.println("Predicted global average score (approx) = " + predictedAvg);

        // --- 5) launch multi-thread tasks: one task per book ---
        int poolSize = Math.min(THREADS, MAX_BOOK_ID);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (int bookId : bookIds) {
            final int bId = bookId;
            final int target = bookRatings.get(bId);
            final double[] probs = bookScoreProbs.get(bId);

            executor.submit(() -> {
                Random rnd = new Random();
                List<Query> queries = new ArrayList<>(BATCH_SIZE);

                // compute start offset in shuffledUsers for this book
                int start = (int) (((long) bId * OFFSET_PRIME) % NUM_USERS);

                // take 'target' consecutive users from shuffledUsers (wrap-around)
                for (int k = 0; k < target; k++) {
                    int idx = start + k;
                    if (idx >= NUM_USERS) {
                        idx -= NUM_USERS;
                    }
                    int accountId = shuffledUsers[idx];

                    int score = sampleScoreFromProbs(probs, rnd);

                    queries.add(
                            dsl.insertInto(RATING)
                                    .set(RATING.ACCOUNT_ID, accountId)
                                    .set(RATING.BOOK_ID, bId)
                                    .set(RATING.SCORE, score)
                                    .onConflictDoNothing()
                    );

                    if (queries.size() >= BATCH_SIZE) {
                        try {
                            dsl.batch(queries).execute();
                        } catch (Exception ex) {
                            System.err.println("Batch execute error for book " + bId + ": " + ex.getMessage());
                        }
                        queries.clear();
                    }
                }

                if (!queries.isEmpty()) {
                    try {
                        dsl.batch(queries).execute();
                    } catch (Exception ex) {
                        System.err.println("Final batch error for book " + bId + ": " + ex.getMessage());
                    }
                    queries.clear();
                }

                System.out.println("Book " + bId + " done (target=" + target + ")");
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }

        System.out.println("✅ Seeder complete. PredictedAvg ≈ " + predictedAvg);
    }

    // --- helper: build probability vector for scores 1..5 from gaussian centered at mu with width sigma
    private double[] makeScoreProbabilities(double mu, double sigma) {
        double[] probs = new double[6]; // index 1..5
        double sum = 0.0;
        for (int k = 1; k <= 5; k++) {
            double diff = k - mu;
            double w = Math.exp(-(diff * diff) / (2.0 * sigma * sigma));
            probs[k] = w;
            sum += w;
        }
        for (int k = 1; k <= 5; k++) {
            probs[k] /= sum;
        }
        return probs;
    }

    // --- sample score 1..5 from precomputed probs
    private int sampleScoreFromProbs(double[] probs, Random rnd) {
        double r = rnd.nextDouble();
        double cum = 0.0;
        for (int k = 1; k <= 5; k++) {
            cum += probs[k];
            if (r <= cum) {
                return k;
            }
        }
        return 5;
    }
    
    // cho mỗi sách: tối thiểu và tối đa số user có progress
    private final int MIN_PROGRESS_PER_BOOK = 50;
    private final int MAX_PROGRESS_PER_BOOK = 20_000;

    // % sách mà ta muốn có nhiều progress (tỷ lệ hot view)
    private final double PERCENT_POPULAR = 0.10;

    // Độ phân bố read chapters: min số chương đọc tối thiểu 1, tối đa là totalChapters (có thể skip)
    private final double MIN_READ_RATIO = 0.05;   // nếu book có 100 chương, min đọc ~5 chương
    private final double MAX_READ_RATIO = 0.9;    // tối đa đọc tới 90% chương (còn có thể skip)

    @Get("/seeder-reading-progress")
    public void seedReadingProgress() {
        Random globalRnd = new Random();

        // 1) lấy số chapter cho mỗi book
        // trả về map book_id -> chapter_count
        Map<Integer, Integer> bookChapterCounts = dsl.select(CHAPTER.BOOK_ID, org.jooq.impl.DSL.count())
                .from(CHAPTER)
                .groupBy(CHAPTER.BOOK_ID)
                .fetchMap(r -> r.get(CHAPTER.BOOK_ID), r -> r.get(org.jooq.impl.DSL.count()));

        if (bookChapterCounts.isEmpty()) {
            System.out.println("No chapters found. Aborting.");
            return;
        }

        // 2) lấy tổng rating hiện có -> để đảm bảo total book_view > total_rating
        long totalRatings = dsl.fetchCount(RATING);
        System.out.println("Existing total ratings = " + totalRatings);

        // 3) xây dựng danh sách bookIds (có chương) và xác định target progress per book
        List<Integer> bookIds = new ArrayList<>(bookChapterCounts.keySet());
        Collections.sort(bookIds); // deterministic order
        Collections.shuffle(bookIds, globalRnd);

        int numPopular = Math.max(1, (int) Math.round(bookIds.size() * PERCENT_POPULAR));

        // compute raw targets by weight: popular books get large weight
        Map<Integer, Integer> bookTargets = new LinkedHashMap<>();
        for (int idx = 0; idx < bookIds.size(); idx++) {
            int bookId = bookIds.get(idx);
            boolean popular = idx < numPopular;
            int base = MIN_PROGRESS_PER_BOOK;
            int extra = globalRnd.nextInt(MAX_PROGRESS_PER_BOOK - MIN_PROGRESS_PER_BOOK + 1);
            int target = base + (popular ? (int) (extra * 1.5) : extra); // popular biased up
            target = Math.min(target, NUM_USERS); // cannot exceed number of users
            bookTargets.put(bookId, target);
        }

        // ensure total planned book_views > totalRatings: if not, scale up proportionally
        long planned = bookTargets.values().stream().mapToLong(Integer::intValue).sum();
        if (planned <= totalRatings) {
            double factor = ((double) totalRatings + 1000) / Math.max(1.0, (double) planned); // +1000 buffer
            System.out.println("Scaling up planned progress by factor " + factor + " to exceed totalRatings");
            for (Map.Entry<Integer, Integer> e : new ArrayList<>(bookTargets.entrySet())) {
                int scaled = (int) Math.min(NUM_USERS, Math.round(e.getValue() * factor));
                bookTargets.put(e.getKey(), Math.max(e.getValue(), scaled));
            }
            planned = bookTargets.values().stream().mapToLong(Integer::intValue).sum();
        }
        System.out.println("Planned total progress entries = " + planned + " (must be > totalRatings)");

        // 4) build shuffledUsers array once
        int[] shuffledUsers = new int[NUM_USERS];
        for (int i = 0; i < NUM_USERS; i++) shuffledUsers[i] = i + 1;
        // Fisher-Yates
        for (int i = NUM_USERS - 1; i > 0; i--) {
            int j = globalRnd.nextInt(i + 1);
            int t = shuffledUsers[i]; shuffledUsers[i] = shuffledUsers[j]; shuffledUsers[j] = t;
        }

        // 5) precompute for each book how many chapters and bytes needed
        Map<Integer, Integer> bookBytesNeeded = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : bookChapterCounts.entrySet()) {
            int chapters = e.getValue();
            int bytes = (chapters + 7) / 8;
            bookBytesNeeded.put(e.getKey(), Math.max(1, bytes));
        }

        // 6) run tasks: one task per book (pool size = min(THREADS, bookCount))
        int poolSize = Math.min(THREADS, bookIds.size());
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (int bookId : bookIds) {
            final int bId = bookId;
            final int target = bookTargets.get(bId);
            final int chaptersCount = bookChapterCounts.get(bId);
            final int bytesNeeded = bookBytesNeeded.get(bId);

            executor.submit(() -> {
                Random rnd = new Random();
                List<Query> queries = new ArrayList<>(BATCH_SIZE);

                // compute start offset in shuffledUsers for this book
                int start = (int) (((long) bId * OFFSET_PRIME) % NUM_USERS);

                // choose target consecutive users from shuffledUsers (wrap-around)
                for (int k = 0; k < target; k++) {
                    int idx = start + k;
                    if (idx >= NUM_USERS) idx -= NUM_USERS;
                    int accountId = shuffledUsers[idx];

                    // decide how many chapters this user has read for this book
                    int minRead = Math.max(1, (int) Math.round(chaptersCount * MIN_READ_RATIO));
                    int maxRead = Math.max(minRead, (int) Math.round(chaptersCount * MAX_READ_RATIO));
                    int readCount = minRead + rnd.nextInt(Math.max(1, maxRead - minRead + 1));

                    // pick 'readCount' distinct chapter positions (1..chaptersCount) — allow skips
                    int[] picks = pickUniqueIndices(chaptersCount, readCount, rnd);

                    // build byte[] bitmap
                    byte[] bitmap = makeBitmapFromPositions(picks, chaptersCount);

                    // insert/upsert reading_progress — overwrite previous bitmap (if any)
                    queries.add(
                        dsl.insertInto(READING_PROGRESS)
                           .set(READING_PROGRESS.ACCOUNT_ID, accountId)
                           .set(READING_PROGRESS.BOOK_ID, bId)
                           .set(READING_PROGRESS.READ_CHAPTERS_BITMAP, bitmap)
                           .set(READING_PROGRESS.LAST_UPDATED_AT, OffsetDateTime.now())
                           .onConflict(READING_PROGRESS.ACCOUNT_ID, READING_PROGRESS.BOOK_ID)
                           .doUpdate()
                           .set(READING_PROGRESS.READ_CHAPTERS_BITMAP, bitmap)
                           .set(READING_PROGRESS.LAST_UPDATED_AT, OffsetDateTime.now())
                    );

                    // upsert book_view (set viewed_at to now)
                    queries.add(
                        dsl.insertInto(BOOK_VIEW)
                           .set(BOOK_VIEW.BOOK_ID, bId)
                           .set(BOOK_VIEW.ACCOUNT_ID, accountId)
                           .set(BOOK_VIEW.VIEWED_AT, OffsetDateTime.now())
                           .onConflict(BOOK_VIEW.ACCOUNT_ID, BOOK_VIEW.BOOK_ID)
                           .doUpdate()
                           .set(BOOK_VIEW.VIEWED_AT, OffsetDateTime.now())
                    );

                    // batch execute periodically
                    if (queries.size() >= BATCH_SIZE) {
                        try {
                            dsl.batch(queries).execute();
                        } catch (Exception ex) {
                            System.err.println("Batch error for book " + bId + ": " + ex.getMessage());
                        }
                        queries.clear();
                    }
                }

                // flush remaining
                if (!queries.isEmpty()) {
                    try {
                        dsl.batch(queries).execute();
                    } catch (Exception ex) {
                        System.err.println("Final batch error for book " + bId + ": " + ex.getMessage());
                    }
                    queries.clear();
                }

                System.out.println("Book " + bId + " done (target=" + target + ", chapters=" + chaptersCount + ")");
            });
        }

        // wait tasks finish
        executor.shutdown();
        while (!executor.isTerminated()) {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        // final stats
        long finalBookViews = dsl.fetchCount(BOOK_VIEW);
        long finalRatings = dsl.fetchCount(RATING);
        System.out.println("Seeding done. book_view=" + finalBookViews + ", rating=" + finalRatings);
        if (finalBookViews <= finalRatings) {
            System.out.println("Warning: book_view <= rating (consider increasing MAX_PROGRESS_PER_BOOK or PERCENT_POPULAR).");
        }
    }

    // --- helper: pick `count` unique indices from 1..n (inclusive).
    // If count >= n, return 1..n.
    private static int[] pickUniqueIndices(int n, int count, Random rnd) {
        if (count >= n) {
            int[] all = new int[n];
            for (int i = 0; i < n; i++) all[i] = i + 1;
            return all;
        }
        // If count is small relative to n, use HashSet sampling
        int[] result = new int[count];
        if (count * 3 < n) {
            Set<Integer> s = new HashSet<>(count);
            while (s.size() < count) {
                s.add(1 + rnd.nextInt(n));
            }
            int i = 0;
            for (int v : s) result[i++] = v;
            return result;
        }
        // Otherwise shuffle array and take first `count`
        int[] pool = new int[n];
        for (int i = 0; i < n; i++) pool[i] = i + 1;
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = pool[i]; pool[i] = pool[j]; pool[j] = tmp;
        }
        System.arraycopy(pool, 0, result, 0, count);
        return result;
    }

    // --- helper: build byte[] bitmap from positions (1-based chapter positions), length = ceil(chapters/8)
    private static byte[] makeBitmapFromPositions(int[] positions, int chaptersCount) {
        int bytes = (chaptersCount + 7) / 8;
        byte[] bitmap = new byte[Math.max(1, bytes)];
        // We adopt convention: bitIndex = position-1, set bit (7-(bitIndex%8)) in byte at index (bitIndex/8)
        // This matches common "set_bit" usage where bit 0 is most significant within a byte.
        for (int pos : positions) {
            int bitIndex = pos - 1;
            int byteIndex = bitIndex / 8;
            int bitInByte = 7 - (bitIndex % 8);
            bitmap[byteIndex] |= (1 << bitInByte);
        }
        return bitmap;
    }
}
