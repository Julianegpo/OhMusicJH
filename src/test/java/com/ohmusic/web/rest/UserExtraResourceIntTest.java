package com.ohmusic.web.rest;

import com.ohmusic.OhMusicApp;

import com.ohmusic.domain.UserExtra;
import com.ohmusic.domain.User;
import com.ohmusic.repository.UserExtraRepository;
import com.ohmusic.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;


import static com.ohmusic.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the UserExtraResource REST controller.
 *
 * @see UserExtraResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OhMusicApp.class)
public class UserExtraResourceIntTest {

    private static final LocalDate DEFAULT_BIRTH_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_BIRTH_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final Integer DEFAULT_TYPE = 1;
    private static final Integer UPDATED_TYPE = 2;

    @Autowired
    private UserExtraRepository userExtraRepository;


    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restUserExtraMockMvc;

    private UserExtra userExtra;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final UserExtraResource userExtraResource = new UserExtraResource(userExtraRepository);
        this.restUserExtraMockMvc = MockMvcBuilders.standaloneSetup(userExtraResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserExtra createEntity(EntityManager em) {
        UserExtra userExtra = new UserExtra()
            .birthDate(DEFAULT_BIRTH_DATE)
            .type(DEFAULT_TYPE);
        // Add required entity
        User user = UserResourceIntTest.createEntity(em);
        em.persist(user);
        em.flush();
        userExtra.setUser(user);
        return userExtra;
    }

    @Before
    public void initTest() {
        userExtra = createEntity(em);
    }

    @Test
    @Transactional
    public void createUserExtra() throws Exception {
        int databaseSizeBeforeCreate = userExtraRepository.findAll().size();

        // Create the UserExtra
        restUserExtraMockMvc.perform(post("/api/user-extras")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userExtra)))
            .andExpect(status().isCreated());

        // Validate the UserExtra in the database
        List<UserExtra> userExtraList = userExtraRepository.findAll();
        assertThat(userExtraList).hasSize(databaseSizeBeforeCreate + 1);
        UserExtra testUserExtra = userExtraList.get(userExtraList.size() - 1);
        assertThat(testUserExtra.getBirthDate()).isEqualTo(DEFAULT_BIRTH_DATE);
        assertThat(testUserExtra.getType()).isEqualTo(DEFAULT_TYPE);
    }

    @Test
    @Transactional
    public void createUserExtraWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userExtraRepository.findAll().size();

        // Create the UserExtra with an existing ID
        userExtra.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserExtraMockMvc.perform(post("/api/user-extras")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userExtra)))
            .andExpect(status().isBadRequest());

        // Validate the UserExtra in the database
        List<UserExtra> userExtraList = userExtraRepository.findAll();
        assertThat(userExtraList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkBirthDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = userExtraRepository.findAll().size();
        // set the field null
        userExtra.setBirthDate(null);

        // Create the UserExtra, which fails.

        restUserExtraMockMvc.perform(post("/api/user-extras")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userExtra)))
            .andExpect(status().isBadRequest());

        List<UserExtra> userExtraList = userExtraRepository.findAll();
        assertThat(userExtraList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTypeIsRequired() throws Exception {
        int databaseSizeBeforeTest = userExtraRepository.findAll().size();
        // set the field null
        userExtra.setType(null);

        // Create the UserExtra, which fails.

        restUserExtraMockMvc.perform(post("/api/user-extras")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userExtra)))
            .andExpect(status().isBadRequest());

        List<UserExtra> userExtraList = userExtraRepository.findAll();
        assertThat(userExtraList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllUserExtras() throws Exception {
        // Initialize the database
        userExtraRepository.saveAndFlush(userExtra);

        // Get all the userExtraList
        restUserExtraMockMvc.perform(get("/api/user-extras?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userExtra.getId().intValue())))
            .andExpect(jsonPath("$.[*].birthDate").value(hasItem(DEFAULT_BIRTH_DATE.toString())))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE)));
    }
    

    @Test
    @Transactional
    public void getUserExtra() throws Exception {
        // Initialize the database
        userExtraRepository.saveAndFlush(userExtra);

        // Get the userExtra
        restUserExtraMockMvc.perform(get("/api/user-extras/{id}", userExtra.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(userExtra.getId().intValue()))
            .andExpect(jsonPath("$.birthDate").value(DEFAULT_BIRTH_DATE.toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE));
    }
    @Test
    @Transactional
    public void getNonExistingUserExtra() throws Exception {
        // Get the userExtra
        restUserExtraMockMvc.perform(get("/api/user-extras/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateUserExtra() throws Exception {
        // Initialize the database
        userExtraRepository.saveAndFlush(userExtra);

        int databaseSizeBeforeUpdate = userExtraRepository.findAll().size();

        // Update the userExtra
        UserExtra updatedUserExtra = userExtraRepository.findById(userExtra.getId()).get();
        // Disconnect from session so that the updates on updatedUserExtra are not directly saved in db
        em.detach(updatedUserExtra);
        updatedUserExtra
            .birthDate(UPDATED_BIRTH_DATE)
            .type(UPDATED_TYPE);

        restUserExtraMockMvc.perform(put("/api/user-extras")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedUserExtra)))
            .andExpect(status().isOk());

        // Validate the UserExtra in the database
        List<UserExtra> userExtraList = userExtraRepository.findAll();
        assertThat(userExtraList).hasSize(databaseSizeBeforeUpdate);
        UserExtra testUserExtra = userExtraList.get(userExtraList.size() - 1);
        assertThat(testUserExtra.getBirthDate()).isEqualTo(UPDATED_BIRTH_DATE);
        assertThat(testUserExtra.getType()).isEqualTo(UPDATED_TYPE);
    }

    @Test
    @Transactional
    public void updateNonExistingUserExtra() throws Exception {
        int databaseSizeBeforeUpdate = userExtraRepository.findAll().size();

        // Create the UserExtra

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restUserExtraMockMvc.perform(put("/api/user-extras")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(userExtra)))
            .andExpect(status().isBadRequest());

        // Validate the UserExtra in the database
        List<UserExtra> userExtraList = userExtraRepository.findAll();
        assertThat(userExtraList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteUserExtra() throws Exception {
        // Initialize the database
        userExtraRepository.saveAndFlush(userExtra);

        int databaseSizeBeforeDelete = userExtraRepository.findAll().size();

        // Get the userExtra
        restUserExtraMockMvc.perform(delete("/api/user-extras/{id}", userExtra.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<UserExtra> userExtraList = userExtraRepository.findAll();
        assertThat(userExtraList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserExtra.class);
        UserExtra userExtra1 = new UserExtra();
        userExtra1.setId(1L);
        UserExtra userExtra2 = new UserExtra();
        userExtra2.setId(userExtra1.getId());
        assertThat(userExtra1).isEqualTo(userExtra2);
        userExtra2.setId(2L);
        assertThat(userExtra1).isNotEqualTo(userExtra2);
        userExtra1.setId(null);
        assertThat(userExtra1).isNotEqualTo(userExtra2);
    }
}
