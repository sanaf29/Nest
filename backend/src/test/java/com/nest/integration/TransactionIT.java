package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionIT extends AbstractIntegrationTest {

    private String token;
    private long chequing;
    private long groceries;
    private long salary;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin();
        chequing = createAccount(token, "Chequing", "CHECKING", "1000.00", null);
        groceries = defaultCategory(token, "Groceries");
        salary = defaultCategory(token, "Salary");
    }

    // ---- helpers ----------------------------------------------------------------------------

    private long createAccount(String userToken, String name, String type, String opening, String currency) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("name", name, "accountType", type, "openingBalance", opening));
        if (currency != null) {
            body.put("currency", currency);
        }
        return body(mockMvc.perform(post("/api/accounts").header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private long defaultCategory(String userToken, String name) throws Exception {
        JsonNode list = body(mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andReturn());
        for (JsonNode c : list) {
            if (c.get("name").asText().equals(name)) {
                return c.get("id").asLong();
            }
        }
        throw new IllegalStateException("No default category " + name);
    }

    private static Map<String, Object> tx(long accountId, long categoryId, String type, String amount, String date, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("accountId", accountId);
        body.put("categoryId", categoryId);
        body.put("type", type);
        body.put("amount", amount);
        body.put("transactionDate", date);
        body.put("description", description);
        return body;
    }

    private ResultActions postTx(String userToken, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/transactions").header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private long createTx(Map<String, Object> body) throws Exception {
        return body(postTx(token, body).andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private ResultActions balanceOf(long accountId) throws Exception {
        return mockMvc.perform(get("/api/accounts/{id}", accountId).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    // ---- tests ------------------------------------------------------------------------------

    @Test
    void createReturnsTransactionWithAccountCurrency() throws Exception {
        postTx(token, tx(chequing, groceries, "EXPENSE", "84.3", "2026-10-18", " Weekly groceries "))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/transactions/\\d+")))
                .andExpect(jsonPath("$.amount").value(84.30))
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.categoryName").value("Groceries"))
                .andExpect(jsonPath("$.accountName").value("Chequing"))
                .andExpect(jsonPath("$.description").value("Weekly groceries"))
                .andExpect(jsonPath("$.transactionDate").value("2026-10-18"));
    }

    @Test
    void accountBalanceIsDerivedFromTransactionsAndFollowsEditsAndDeletes() throws Exception {
        createTx(tx(chequing, salary, "INCOME", "3000.00", "2026-10-01", "Pay"));
        long food = createTx(tx(chequing, groceries, "EXPENSE", "245.67", "2026-10-03", "Food"));
        balanceOf(chequing).andExpect(jsonPath("$.currentBalance").value(3754.33)); // 1000 + 3000 - 245.67

        mockMvc.perform(put("/api/transactions/{id}", food).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(tx(chequing, groceries, "EXPENSE", "200.00", "2026-10-03", "Food"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200.00));
        balanceOf(chequing).andExpect(jsonPath("$.currentBalance").value(3800.00));

        mockMvc.perform(delete("/api/transactions/{id}", food).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        balanceOf(chequing).andExpect(jsonPath("$.currentBalance").value(4000.00));
        mockMvc.perform(get("/api/transactions/{id}", food).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listedAccountsShowTheirOwnBalances() throws Exception {
        long visa = createAccount(token, "Visa", "CREDIT_CARD", "0", null);
        createTx(tx(chequing, salary, "INCOME", "500", "2026-10-01", null));
        createTx(tx(visa, groceries, "EXPENSE", "120.40", "2026-10-02", null));

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$[?(@.name == 'Chequing')].currentBalance", contains(1500.00)))
                .andExpect(jsonPath("$[?(@.name == 'Visa')].currentBalance", contains(-120.40)));
    }

    @Nested
    class BusinessRules {

        @Test
        void categoryMustMatchTheTransactionType() throws Exception {
            postTx(token, tx(chequing, salary, "EXPENSE", "10", "2026-10-01", null))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value(
                            "Category 'Salary' is a INCOME category and cannot classify a EXPENSE transaction."));
        }

        @Test
        void archivedCategoryCannotBeUsed() throws Exception {
            long pets = body(mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("name", "Pets", "categoryType", "EXPENSE")))).andReturn()).get("id").asLong();
            long used = createTx(tx(chequing, pets, "EXPENSE", "30", "2026-10-01", null));
            mockMvc.perform(delete("/api/categories/{id}", pets).header(HttpHeaders.AUTHORIZATION, bearer(token)));

            postTx(token, tx(chequing, pets, "EXPENSE", "10", "2026-10-02", null))
                    .andExpect(status().isUnprocessableEntity());
            // The existing transaction keeps its archived category.
            mockMvc.perform(get("/api/transactions/{id}", used).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(jsonPath("$.categoryName").value("Pets"));
        }

        @Test
        void closedAccountHistoryIsFrozen() throws Exception {
            long recorded = createTx(tx(chequing, groceries, "EXPENSE", "10", "2026-10-01", null));
            mockMvc.perform(delete("/api/accounts/{id}", chequing).header(HttpHeaders.AUTHORIZATION, bearer(token)));

            postTx(token, tx(chequing, groceries, "EXPENSE", "10", "2026-10-02", null))
                    .andExpect(status().isUnprocessableEntity());
            mockMvc.perform(put("/api/transactions/{id}", recorded).header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(tx(chequing, groceries, "EXPENSE", "11", "2026-10-01", null))))
                    .andExpect(status().isUnprocessableEntity());
            mockMvc.perform(delete("/api/transactions/{id}", recorded).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnprocessableEntity());
            mockMvc.perform(get("/api/transactions/{id}", recorded).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk());
        }

        @Test
        void currencyClaimAndPrecisionAreChecked() throws Exception {
            Map<String, Object> wrongCurrency = tx(chequing, groceries, "EXPENSE", "10", "2026-10-01", null);
            wrongCurrency.put("currency", "USD");
            postTx(token, wrongCurrency).andExpect(status().isUnprocessableEntity());

            postTx(token, tx(chequing, groceries, "EXPENSE", "10.555", "2026-10-01", null))
                    .andExpect(status().isUnprocessableEntity());

            long yen = createAccount(token, "Yen", "CASH", "0", "JPY");
            postTx(token, tx(yen, groceries, "EXPENSE", "10.5", "2026-10-01", null))
                    .andExpect(status().isUnprocessableEntity());
            postTx(token, tx(yen, groceries, "EXPENSE", "1500", "2026-10-01", null))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currency").value("JPY"));
        }

        @Test
        void invalidAmountsAndMissingFieldsAreBadRequests() throws Exception {
            postTx(token, tx(chequing, groceries, "EXPENSE", "0", "2026-10-01", null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("amount"));
            postTx(token, tx(chequing, groceries, "EXPENSE", "-10", "2026-10-01", null))
                    .andExpect(status().isBadRequest());
            postTx(token, Map.of("type", "EXPENSE"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void transfersCannotBeCreatedHere() throws Exception {
            postTx(token, tx(chequing, groceries, "TRANSFER_OUT", "10", "2026-10-01", null))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value("Transfers must be recorded through /api/transfers."));
        }
    }

    @Nested
    class Isolation {

        @Test
        void anotherUserCannotSeeUseOrChangeMyTransactions() throws Exception {
            long mine = createTx(tx(chequing, groceries, "EXPENSE", "10", "2026-10-01", "private"));
            String other = registerAndLogin();
            long theirAccount = createAccount(other, "Theirs", "CHECKING", "0", null);

            mockMvc.perform(get("/api/transactions/{id}", mine).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(put("/api/transactions/{id}", mine).header(HttpHeaders.AUTHORIZATION, bearer(other))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(tx(theirAccount, groceries, "EXPENSE", "1", "2026-10-01", null))))
                    .andExpect(status().isNotFound());
            mockMvc.perform(delete("/api/transactions/{id}", mine).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/api/transactions").header(HttpHeaders.AUTHORIZATION, bearer(other)))
                    .andExpect(jsonPath("$.totalElements").value(0));
            // Recording against my account is refused, so my balance cannot be changed.
            postTx(other, tx(chequing, groceries, "EXPENSE", "999", "2026-10-01", null))
                    .andExpect(status().isNotFound());
            balanceOf(chequing).andExpect(jsonPath("$.currentBalance").value(990.00));
        }

        @Test
        void myTransactionCannotBeMovedOntoAnotherUsersAccount() throws Exception {
            long mine = createTx(tx(chequing, groceries, "EXPENSE", "10", "2026-10-01", null));
            String other = registerAndLogin();
            long theirAccount = createAccount(other, "Theirs", "CHECKING", "0", null);

            mockMvc.perform(put("/api/transactions/{id}", mine).header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(tx(theirAccount, groceries, "EXPENSE", "10", "2026-10-01", null))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void anotherUsersCategoryCannotBeUsed() throws Exception {
            String other = registerAndLogin();
            long theirCategory = body(mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer(other))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("name", "Secret", "categoryType", "EXPENSE")))).andReturn()).get("id").asLong();

            postTx(token, tx(chequing, theirCategory, "EXPENSE", "10", "2026-10-01", null))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Search {

        private long savings;

        @BeforeEach
        void seed() throws Exception {
            savings = createAccount(token, "Savings", "SAVINGS", "0", null);
            createTx(tx(chequing, salary, "INCOME", "3000", "2026-09-30", "September pay"));
            createTx(tx(chequing, groceries, "EXPENSE", "50.00", "2026-10-01", "Farmers market"));
            createTx(tx(chequing, groceries, "EXPENSE", "120.00", "2026-10-15", "Costco run"));
            createTx(tx(savings, salary, "INCOME", "25.00", "2026-10-20", "Interest 100%_bonus"));
        }

        private ResultActions search(String query) throws Exception {
            return mockMvc.perform(get("/api/transactions?" + query).header(HttpHeaders.AUTHORIZATION, bearer(token)));
        }

        @Test
        void defaultsToNewestFirst() throws Exception {
            search("").andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(4))
                    .andExpect(jsonPath("$.content[*].transactionDate",
                            contains("2026-10-20", "2026-10-15", "2026-10-01", "2026-09-30")));
        }

        @Test
        void filtersCombineWithAnd() throws Exception {
            search("type=EXPENSE&from=2026-10-01&to=2026-10-31")
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[*].type", everyItem(is("EXPENSE"))));
            search("accountId=" + savings).andExpect(jsonPath("$.totalElements").value(1));
            search("categoryId=" + groceries + "&minAmount=100").andExpect(jsonPath("$.content[*].amount", contains(120.00)));
            search("maxAmount=50").andExpect(jsonPath("$.content[*].amount", contains(25.00, 50.00)));
            search("q=COSTCO").andExpect(jsonPath("$.content[*].description", contains("Costco run")));
        }

        @Test
        void textSearchTreatsWildcardsLiterally() throws Exception {
            // Unescaped, '%' and '_' would match every row; they must only match themselves.
            for (String literal : new String[]{"%", "100%_", "_"}) {
                mockMvc.perform(get("/api/transactions").param("q", literal).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                        .andExpect(jsonPath("$.content[*].description", contains("Interest 100%_bonus")));
            }
        }

        @Test
        void paginatesAndSorts() throws Exception {
            search("size=2&page=1&sort=amount,asc")
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.content[*].amount", contains(120.00, 3000.00)));
        }

        @Test
        void invalidPagingSortingAndRangesAreBadRequests() throws Exception {
            search("size=101").andExpect(status().isBadRequest());
            search("page=-1").andExpect(status().isBadRequest());
            search("sort=passwordHash").andExpect(status().isBadRequest());
            search("from=2026-10-31&to=2026-10-01").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].message", hasItem("'from' must not be after 'to'")));
            search("minAmount=10&maxAmount=5").andExpect(status().isBadRequest());
            search("type=REFUND").andExpect(status().isBadRequest());
            search("from=yesterday").andExpect(status().isBadRequest());
        }

        @Test
        void otherUsersRowsNeverAppear() throws Exception {
            String other = registerAndLogin();
            long theirs = createAccount(other, "Theirs", "CHECKING", "0", null);
            postTx(other, tx(theirs, groceries, "EXPENSE", "77.77", "2026-10-05", "Costco run")).andExpect(status().isCreated());

            search("q=costco").andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[*].amount", not(hasItem(77.77))));
        }
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
    }
}
