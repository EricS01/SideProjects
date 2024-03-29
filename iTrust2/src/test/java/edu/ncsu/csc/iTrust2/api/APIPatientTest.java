package edu.ncsu.csc.iTrust2.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import edu.ncsu.csc.iTrust2.common.TestUtils;
import edu.ncsu.csc.iTrust2.forms.PatientForm;
import edu.ncsu.csc.iTrust2.forms.UserForm;
import edu.ncsu.csc.iTrust2.forms.VaccineTypeForm;
import edu.ncsu.csc.iTrust2.models.Patient;
import edu.ncsu.csc.iTrust2.models.enums.BloodType;
import edu.ncsu.csc.iTrust2.models.enums.Ethnicity;
import edu.ncsu.csc.iTrust2.models.enums.Gender;
import edu.ncsu.csc.iTrust2.models.enums.Role;
import edu.ncsu.csc.iTrust2.models.enums.State;
import edu.ncsu.csc.iTrust2.models.enums.VaccinationStatus;
import edu.ncsu.csc.iTrust2.services.PatientService;

/**
 * Test for API functionality for interacting with Patients
 *
 * @author Kai Presler-Marshall
 *
 */
@ExtendWith ( SpringExtension.class )
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ( { "test" } )
public class APIPatientTest {

    @Autowired
    private MockMvc                 mvc;

    @Autowired
    private PatientService<Patient> service;

    /**
     * Sets up test
     */
    @BeforeEach
    public void setup () {

        service.deleteAll();
    }

    /**
     * Tests that getting a patient that doesn't exist returns the proper
     * status.
     *
     * @throws Exception
     */
    @Test
    @Transactional
    @WithMockUser ( username = "hcp", roles = { "HCP" } )
    public void testGetNonExistentPatient () throws Exception {
        mvc.perform( get( "/api/v1/patients/-1" ) ).andExpect( status().isNotFound() );
    }

    /**
     * Tests PatientAPI
     *
     * @throws Exception
     */
    @Test
    @WithMockUser ( username = "hcp", roles = { "HCP" } )
    @Transactional
    public void testPatientAPI () throws Exception {

        final PatientForm patient = new PatientForm();
        patient.setAddress1( "1 Test Street" );
        patient.setAddress2( "Some Location" );
        patient.setBloodType( BloodType.APos.toString() );
        patient.setCity( "Viipuri" );
        patient.setDateOfBirth( "1977-06-15" );
        patient.setEmail( "antti@itrust.fi" );
        patient.setEthnicity( Ethnicity.Caucasian.toString() );
        patient.setFirstName( "Antti" );
        patient.setGender( Gender.Male.toString() );
        patient.setLastName( "Walhelm" );
        patient.setPhone( "123-456-7890" );
        patient.setUsername( "antti" );
        patient.setState( State.NC.toString() );
        patient.setZip( "27514" );
        patient.setVaccinationStatus( VaccinationStatus.FIRST_DOSE.toString() );

        // Editing the patient before they exist should fail
        mvc.perform( put( "/api/v1/patients/antti" ).with( csrf() ).contentType( MediaType.APPLICATION_JSON )
                .content( TestUtils.asJsonString( patient ) ) ).andExpect( status().isNotFound() );

        final Patient antti = new Patient( new UserForm( "antti", "123456", Role.ROLE_PATIENT, 1 ) );

        service.save( antti );

        // Creating a User should create the Patient record automatically
        mvc.perform( get( "/api/v1/patients/antti" ) ).andExpect( status().isOk() );

        // Should also now be able to edit existing record with new information
        mvc.perform( put( "/api/v1/patients/antti" ).with( csrf() ).contentType( MediaType.APPLICATION_JSON )
                .content( TestUtils.asJsonString( patient ) ) ).andExpect( status().isOk() );

        Patient anttiRetrieved = (Patient) service.findByName( "antti" );

        // Test a few fields to be reasonably confident things are working
        Assertions.assertEquals( "Walhelm", anttiRetrieved.getLastName() );
        Assertions.assertEquals( Gender.Male, anttiRetrieved.getGender() );
        Assertions.assertEquals( "Viipuri", anttiRetrieved.getCity() );

        // Also test a field we haven't set yet
        Assertions.assertNull( anttiRetrieved.getPreferredName() );

        patient.setPreferredName( "Antti Walhelm" );

        mvc.perform( put( "/api/v1/patients/antti" ).with( csrf() ).contentType( MediaType.APPLICATION_JSON )
                .content( TestUtils.asJsonString( patient ) ) ).andExpect( status().isOk() );

        anttiRetrieved = (Patient) service.findByName( "antti" );

        Assertions.assertNotNull( anttiRetrieved.getPreferredName() );

        // .5

        // Editing with the wrong username should fail.
        mvc.perform( put( "/api/v1/patients/badusername" ).with( csrf() ).contentType( MediaType.APPLICATION_JSON )
                .content( TestUtils.asJsonString( patient ) ) ).andExpect( status().is4xxClientError() );
        mvc.perform( get( "/api/v1/vaccinationstatus" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        mvc.perform( get( "/api/v1/bloodtype" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        mvc.perform( get( "/api/v1/ethnicity" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        mvc.perform( get( "/api/v1/gender" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        mvc.perform( get( "/api/v1/state" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        // mvc.perform( get( "/api/v1/appointmenttype" ) ).andExpect(
        // status().isOk() ).andReturn().getResponse()
        // .getContentAsString();
        mvc.perform( get( "/api/v1/state" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        mvc.perform( get( "/api/v1/appointmentstatus" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        mvc.perform( get( "/api/v1/housesmoking" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        mvc.perform( get( "/api/v1/patientsmoking" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();

        mvc.perform( post( "/api/v1/generateUsers" ) ).andExpect( status().isOk() );

        mvc.perform( get( "/api/v1/viewAppointments" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        mvc.perform( get( "/api/v1/appointmentrequestForHCP" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        mvc.perform( get( "/api/v1/officevisits/HCP" ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString();
        // mvc.perform( get( "/api/v1/prescriptions/antti" ) ).andExpect(
        // status().isOk() ).andReturn().getResponse()
        // .getContentAsString();

    }

    /**
     * Test accessing the patient PUT request unauthenticated
     *
     * @throws Exception
     */
    @Test
    @Transactional
    @WithMockUser ( username = "user", roles = { "USER" } )
    public void testPatientUnauthenticated () throws Exception {
        final PatientForm patient = new PatientForm();
        patient.setAddress1( "1 Test Street" );
        patient.setAddress2( "Some Location" );
        patient.setBloodType( BloodType.APos.toString() );
        patient.setCity( "Viipuri" );
        patient.setDateOfBirth( "1977-06-15" );
        patient.setEmail( "antti@itrust.fi" );
        patient.setEthnicity( Ethnicity.Caucasian.toString() );
        patient.setFirstName( "Antti" );
        patient.setGender( Gender.Male.toString() );
        patient.setLastName( "Walhelm" );
        patient.setPhone( "123-456-7890" );
        patient.setUsername( "antti" );
        patient.setState( State.NC.toString() );
        patient.setZip( "27514" );
        patient.setVaccinationStatus( VaccinationStatus.FIRST_DOSE.toString() );

        final Patient antti = new Patient( new UserForm( "antti", "123456", Role.ROLE_PATIENT, 1 ) );

        service.save( antti );

        mvc.perform( put( "/api/v1/patients/antti" ).with( csrf() ).contentType( MediaType.APPLICATION_JSON )
                .content( TestUtils.asJsonString( patient ) ) ).andExpect( status().isUnauthorized() );
    }

    /**
     * Test accessing the patient PUT request as a patient
     *
     * @throws Exception
     */
    @Test
    @Transactional
    @WithMockUser ( username = "antti", roles = { "PATIENT" } )
    public void testPatientAsPatient () throws Exception {
        final Patient antti = new Patient( new UserForm( "antti", "123456", Role.ROLE_PATIENT, 1 ) );

        service.save( antti );

        final PatientForm patient = new PatientForm();
        patient.setAddress1( "1 Test Street" );
        patient.setAddress2( "Some Location" );
        patient.setBloodType( BloodType.APos.toString() );
        patient.setCity( "Viipuri" );
        patient.setDateOfBirth( "1977-06-15" );
        patient.setEmail( "antti@itrust.fi" );
        patient.setEthnicity( Ethnicity.Caucasian.toString() );
        patient.setFirstName( "Antti" );
        patient.setGender( Gender.Male.toString() );
        patient.setLastName( "Walhelm" );
        patient.setPhone( "123-456-7890" );
        patient.setUsername( "antti" );
        patient.setState( State.NC.toString() );
        patient.setZip( "27514" );
        patient.setVaccinationStatus( VaccinationStatus.FULLY_VACCINATED.toString() );
        patient.setDoses( "1" );
        // patient.setVaccineType( "TestingVacc" );
        final VaccineTypeForm form1 = new VaccineTypeForm();

        form1.setName( "TestingVacc" );
        form1.setNumDoses( 2 );
        form1.setMinAge( 12 );
        form1.setMaxAge( 100 );
        form1.setIsAvailable( true );
        form1.setInventoryAmount( 100 );
        form1.setDaysBetweenDoses( 21 );
        patient.setVaccineType( "TestingVacc" );
        // a patient can edit their own info
        mvc.perform( put( "/api/v1/patients/antti" ).with( csrf() ).contentType( MediaType.APPLICATION_JSON )
                .content( TestUtils.asJsonString( patient ) ) ).andExpect( status().isOk() );

        final Patient anttiRetrieved = (Patient) service.findByName( "antti" );

        // Test a few fields to be reasonably confident things are working
        Assertions.assertEquals( "Walhelm", anttiRetrieved.getLastName() );
        Assertions.assertEquals( Gender.Male, anttiRetrieved.getGender() );
        Assertions.assertEquals( "Viipuri", anttiRetrieved.getCity() );

        // Also test a field we haven't set yet
        Assertions.assertNull( anttiRetrieved.getPreferredName() );

        // but they can't edit someone else's
        patient.setUsername( "patient" );
        mvc.perform( put( "/api/v1/patients/patient" ).with( csrf() ).contentType( MediaType.APPLICATION_JSON )
                .content( TestUtils.asJsonString( patient ) ) ).andExpect( status().isUnauthorized() );

        // we should be able to update our record too
        mvc.perform( get( "/api/v1/patient/" ) ).andExpect( status().isOk() );
        mvc.perform( get( "/api/v1/patients/associated" ) ).andExpect( status().isOk() );
        final String zipcode = mvc.perform( get( "/api/v1/patient/findexperts/getzip" ) ).andExpect( status().isOk() )
                .andReturn().getResponse().getContentAsString();
        // Assertions.assertEquals( "27514", zipcode );

        mvc.perform( put( "/api/v1//patients/updateVaccination/antti/2/TestingVacc" ).with( csrf() )
                .contentType( MediaType.APPLICATION_JSON ).content( TestUtils.asJsonString( patient ) ) )
                .andExpect( status().isBadRequest() );

        mvc.perform( get( "/api/v1/appointmentrequest/approved" ) ).andExpect( status().isOk() );

        mvc.perform( get( "/api/v1/officevisits/myofficevisits" ) ).andExpect( status().isOk() );

        mvc.perform( get( "/api/v1/bills/mybills" ) ).andExpect( status().isOk() );

        // .andExpect( content().contentType( MediaType.APPLICATION_JSON_VALUE )
        // );
    }

    // /**
    // * Test accessing the patient PUT request as a patient
    // *
    // * @throws Exception
    // */
    // @Test
    // @Transactional
    // @WithMockUser ( username = "antti", roles = { "PATIENT" } )
    // public void testGetPatientAssociation () throws Exception {
    // final Patient antti = new Patient( new UserForm( "antti", "123456",
    // Role.ROLE_PATIENT, 1 ) );
    // mvc.perform( put( "/api/v1/patients/associated" ).with( csrf()
    // ).contentType( MediaType.APPLICATION_JSON )
    // .content( TestUtils.asJsonString( antti ) ) ).andExpect( status().isOk()
    // );
    //
    // }

}
