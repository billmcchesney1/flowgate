/**
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: BSD-2-Clause
*/
package com.vmware.flowgate.controller;

import static org.mockito.Matchers.any;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.flowgate.common.model.AuthToken;
import com.vmware.flowgate.common.model.WormholeRole;
import com.vmware.flowgate.common.model.WormholeUser;
import com.vmware.flowgate.exception.WormholeRequestException;
import com.vmware.flowgate.repository.RoleRepository;
import com.vmware.flowgate.repository.UserRepository;
import com.vmware.flowgate.security.service.AccessTokenService;
import com.vmware.flowgate.security.service.UserDetailsServiceImpl;
import com.vmware.flowgate.util.JwtTokenUtil;
import com.vmware.flowgate.util.WormholeUserDetails;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class AuthControllerTest {

   @Rule
   public final JUnitRestDocumentation restDocumentation =
         new JUnitRestDocumentation("target/generated-snippets");

   private MockMvc mockMvc;

   @Autowired
   private ObjectMapper objectMapper;

   @Autowired
   private WebApplicationContext context;
   
   @Autowired
   private RoleRepository roleRepository;
   
   @SpyBean
   private AccessTokenService tokenService;
   
   @SpyBean
   private UserDetailsServiceImpl userDetailservice;
   
   @MockBean
   private StringRedisTemplate template;
   
   @Autowired
   private UserRepository userRepository;
   
   @Rule
   public ExpectedException expectedEx = ExpectedException.none();

   @Before
   public void setUp() {
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation)).build();
   }

   @Test
   public void testCreateToken() throws Exception {
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      WormholeUser wormholeuser = new WormholeUser();
      wormholeuser.setUserName("tom");
      wormholeuser.setPassword("$2a$10$Vm8MLIkGwinuICfcqW5RDOoE.aJqnvsaPhnxl7.N4H7oLKVIu3o0.");
      wormholeuser.setRoleNames(Arrays.asList("admin"));
      userRepository.save(wormholeuser);
      this.mockMvc
      .perform(post("/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
      .content("{\"userName\":\"tom\",\"password\":\"123456\"}"))
      .andExpect(status().isOk())
      .andDo(document("AuthController-CreateAccessToken-example", relaxedRequestFields(
      fieldWithPath("userName").description("A user name for Wormhole Project"),
      fieldWithPath("password").description("A password for Wormhole Project."))))
      .andReturn();

   }

   @Test
   public void testCreateToken1() throws Exception {
      WormholeUser user = null;
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Invalid username or password");
      MvcResult result =  this.mockMvc
      .perform(post("/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(user)))
      .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      }
   }

   @Test
   public void testRefreshToken() throws Exception {
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      WormholeUser wormholeuser = new WormholeUser();
      wormholeuser.setUserName("tom");
      wormholeuser.setPassword("$2a$10$Vm8MLIkGwinuICfcqW5RDOoE.aJqnvsaPhnxl7.N4H7oLKVIu3o0.");
      wormholeuser.setRoleNames(Arrays.asList("admin"));
      userRepository.save(wormholeuser);
      MvcResult result = this.mockMvc
      .perform(post("/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
      .content("{\"userName\":\"tom\",\"password\":\"123456\"}"))
      .andExpect(status().isOk())
      .andReturn();
      String access_token = "";
      Cookie[] cookies =  result.getResponse().getCookies();
      if(cookies != null && cookies.length!=0) {
         for(Cookie currentcookie:cookies) {
            if(JwtTokenUtil.Token_Name.equals(currentcookie.getName())) {
               access_token = currentcookie.getValue();
               break;
            }
         }
      }
      ObjectMapper mapper = new ObjectMapper();
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUsername("tom");
      userdetail.setPassword("$2a$10$Vm8MLIkGwinuICfcqW5RDOoE.aJqnvsaPhnxl7.N4H7oLKVIu3o0.");
      
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      Mockito.doReturn(mapper.writeValueAsString(userdetail)).when(tokenService).getUserJsonString(any());
      MvcResult result1 = this.mockMvc.perform(get("/v1/auth/token/refresh").header("Authorization", "Bearer "+access_token))
      .andDo(document("AuthController-RefreshAccessToken-example"))
      .andReturn();
      TestCase.assertEquals(access_token, result1.getResponse().getHeader("Authorization"));
   }

   @Test
   public void testRefreshToken1() throws Exception {
      AuthToken token = new AuthToken();
      Mockito.doReturn(token).when(tokenService).refreshToken(any());
      this.mockMvc.perform(get("/v1/auth/token/refresh"))
      .andExpect(status().isOk())
      .andReturn();
   }
   
   @Test
   public void testLogout() throws Exception {
      ValueOperations<String, String> valueOperations = Mockito.mock(ValueOperations.class);
      Mockito.doReturn(valueOperations).when(template).opsForValue();
      this.mockMvc.perform(get("/v1/auth/logout").header("Authorization", "Bearer "+"R$TYUIMJ"))
            .andDo(document("AuthController-UserLogout-example"))
            .andExpect(status().isOk())
            .andReturn();
   }
   @Test
   public void createUserExample() throws Exception {

       WormholeUser user = createUser();
       
       this.mockMvc.perform(post("/v1/auth/user").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user)))
              .andExpect(status().isCreated())
              .andDo(document("AuthController-createUser-example", requestFields(
                      fieldWithPath("id").description("ID of User, created by wormhole"),
                      fieldWithPath("userName").description("userName.").type(JsonFieldType.STRING),
                      fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                      fieldWithPath("password").description("password").type(JsonFieldType.STRING),
                      fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                      fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                      fieldWithPath("createTime").description("createTime").type(Date.class),
                      fieldWithPath("emailAddress").description("emailAddress").type(JsonFieldType.STRING),
                      fieldWithPath("roleNames").description("roleNames").type(List.class),
                      fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                      fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate").type(JsonFieldType.NUMBER))))
              .andReturn();
       
   }
   @Test
   public void deleteUserExample() throws Exception {

       WormholeUser user = createUser();
       userRepository.save(user);
       
       this.mockMvc.perform(delete("/v1/auth/user/"+user.getId()+"").content("{\"id\":\"" + user.getId() + "\"}"))
              .andExpect(status().isOk())
              .andDo(document("AuthController-deleteUser-example", requestFields(
                      fieldWithPath("id").description("ID of User, created by wormhole"))))
              .andReturn();
       
   }
   
   @Test
   public void updateUserExample() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId("1001");
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("admin");
      WormholeUser adminUser = new WormholeUser();
      adminUser.setRoleNames(rolenames);
      adminUser.setId("1001");
      userRepository.save(adminUser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      
      WormholeUser user = createUser();
      userRepository.save(user);

      this.mockMvc
            .perform(put("/v1/auth/user").contentType(MediaType.APPLICATION_JSON_VALUE)
                  .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk())
            .andDo(document("AuthController-updateUser-example",
                  requestFields(fieldWithPath("id").description("ID of User, created by wormhole"),
                        fieldWithPath("userName").description("userName.")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                        fieldWithPath("password").description("password")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                        fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                        fieldWithPath("createTime").description("createTime").type(Date.class),
                        fieldWithPath("emailAddress").description("emailAddress")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("roleNames").description("roleNames").type(List.class),
                        fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                        fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate")
                              .type(JsonFieldType.NUMBER))))
            .andReturn();

      userRepository.delete(user.getId());

   }
   
   @Test
   public void updateUserExample1() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId("1001");
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("admin");
      WormholeUser adminUser = new WormholeUser();
      adminUser.setRoleNames(rolenames);
      adminUser.setId("1001");
      userRepository.save(adminUser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      
      WormholeUser user = createUser();
      userRepository.save(user);

      this.mockMvc
            .perform(put("/v1/auth/user").contentType(MediaType.APPLICATION_JSON_VALUE)
                  .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk())
            .andDo(document("AuthController-updateUser-example",
                  requestFields(fieldWithPath("id").description("ID of User, created by wormhole"),
                        fieldWithPath("userName").description("userName.")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                        fieldWithPath("password").description("password")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                        fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                        fieldWithPath("createTime").description("createTime").type(Date.class),
                        fieldWithPath("emailAddress").description("emailAddress")
                              .type(JsonFieldType.STRING),
                        fieldWithPath("roleNames").description("roleNames").type(List.class),
                        fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                        fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate")
                              .type(JsonFieldType.NUMBER))))
            .andReturn();

      userRepository.delete(user.getId());

   }
   
   @Test
   public void readOneUserByIdExample() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId("1001");
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("admin");
      WormholeUser adminUser = new WormholeUser();
      adminUser.setRoleNames(rolenames);
      adminUser.setId("1001");
      userRepository.save(adminUser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      WormholeUser user = createUser();
      userRepository.save(user);

      this.mockMvc.perform(get("/v1/auth/user/"+user.getId()+""))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readOneUserById-example", responseFields(
                      fieldWithPath("id").description("ID of User, created by wormhole"),
                      fieldWithPath("userName").description("userName.").type(JsonFieldType.STRING),
                      fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                      fieldWithPath("password").description("password"),
                      fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                      fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                      fieldWithPath("createTime").description("createTime").type(Date.class),
                      fieldWithPath("emailAddress").description("emailAddress").type(JsonFieldType.STRING),
                      fieldWithPath("roleNames").description("roleNames").type(List.class),
                      fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                      fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate").type(JsonFieldType.NUMBER))))
              .andReturn();
       
       userRepository.delete(user.getId());
   }
   
   @Test
   public void readOneUserByIdExample1() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId("1001");
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("sysuser");
      WormholeUser sysuser = new WormholeUser();
      sysuser.setRoleNames(rolenames);
      sysuser.setId("1001");
      userRepository.save(sysuser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      this.mockMvc.perform(get("/v1/auth/user/"+sysuser.getId()+""))
              .andExpect(status().isOk())
              .andReturn();
       
   }
   
   @Test
   public void readOneUserByIdExample2() throws Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Forbidden");
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId("1001");
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("sysuser");
      WormholeUser sysuser = new WormholeUser();
      sysuser.setRoleNames(rolenames);
      sysuser.setId("1001");
      userRepository.save(sysuser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      MvcResult result = this.mockMvc.perform(get("/v1/auth/user/123"))
              .andExpect(status().isForbidden())
              .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      }
   }
   
   @Test
   public void readOneUserByNameExample() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId("1001");
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("admin");
      WormholeUser adminUser = new WormholeUser();
      adminUser.setRoleNames(rolenames);
      adminUser.setId("1001");
      userRepository.save(adminUser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      WormholeUser user = createUser();
      userRepository.save(user);
      this.mockMvc.perform(get("/v1/auth/user/username/"+user.getUserName()+""))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readOneUserByUserName-example", responseFields(
                    fieldWithPath("id").description("ID of User, created by wormhole"),
                    fieldWithPath("userName").description("userName.").type(JsonFieldType.STRING),
                    fieldWithPath("gender").description("gender").type(JsonFieldType.NUMBER),
                    fieldWithPath("password").description("password"),
                    fieldWithPath("mobile").description("mobile").type(JsonFieldType.STRING),
                    fieldWithPath("status").description("status").type(JsonFieldType.NUMBER),
                    fieldWithPath("createTime").description("createTime").type(Date.class),
                    fieldWithPath("emailAddress").description("emailAddress").type(JsonFieldType.STRING),
                    fieldWithPath("roleNames").description("roleNames").type(List.class),
                    fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
                    fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate").type(JsonFieldType.NUMBER))))
              .andReturn();
       
   }
   
   @Test
   public void readOneUserByUserNameExample1() throws Exception {
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId("1001");
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("sysuser");
      WormholeUser sysuser = new WormholeUser();
      sysuser.setRoleNames(rolenames);
      sysuser.setId("1001");
      sysuser.setUserName("lucy");
      userRepository.save(sysuser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      this.mockMvc.perform(get("/v1/auth/user/username/"+sysuser.getUserName()+""))
              .andExpect(status().isOk())
              .andReturn();
       
   }
   
   @Test
   public void readOneUserByUserNameExample2() throws Exception {
      expectedEx.expect(WormholeRequestException.class);
      expectedEx.expectMessage("Forbidden");
      WormholeUserDetails userdetail = new WormholeUserDetails();
      userdetail.setUserId("1001");
      List<String> rolenames = new ArrayList<String>();
      rolenames.add("sysuser");
      WormholeUser sysuser = new WormholeUser();
      sysuser.setRoleNames(rolenames);
      sysuser.setId("1001");
      sysuser.setUserName("lucy");
      userRepository.save(sysuser);
      Mockito.doReturn(userdetail).when(tokenService).getCurrentUser(any());
      MvcResult result = this.mockMvc.perform(get("/v1/auth/user/username/tom"))
              .andExpect(status().isForbidden())
              .andReturn();
      if (result.getResolvedException() != null) {
         throw result.getResolvedException();
      }
   }
   
   @Test
   public void readUsersByPageExample() throws Exception {

       WormholeUser user1 = createUser();
       user1.setId("user1");
       userRepository.save(user1);

       this.mockMvc.perform(get("/v1/auth/user/page").content("{\"currentPage\":\"1\",\"pageSize\":\"5\"}")
              .param("currentPage", "1").param("pageSize", "5"))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readUsersByPage-example", requestFields(
                      fieldWithPath("currentPage").description("get datas for this page number."),
                      fieldWithPath("pageSize")
                            .description("The number of data displayed per page."))));
       
       userRepository.delete(user1.getId());

   }
   @Test
   public void readAllUsersExample() throws Exception {

       WormholeUser user1 = createUser();
       user1.setId("user1");
       userRepository.save(user1);
       WormholeUser user2 = createUser();
       user2.setId("user2");
       userRepository.save(user2);
       
       FieldDescriptor[] fieldpath = new FieldDescriptor[] {
               fieldWithPath("id").description("ID of User, created by wormhole"),
               fieldWithPath("userName").description("userName."),
               fieldWithPath("gender").description("gender"),
               fieldWithPath("password").description("password"),
               fieldWithPath("mobile").description("mobile"),
               fieldWithPath("status").description("status"),
               fieldWithPath("createTime").description("createTime").type(Date.class),
               fieldWithPath("emailAddress").description("emailAddress"),
               fieldWithPath("roleNames").description("roleNames").type(List.class),
               fieldWithPath("userGroupIDs").description("userGroupIDs").type(List.class),
               fieldWithPath("lastPasswordResetDate").description("lastPasswordResetDate")};
               
       this.mockMvc.perform(get("/v1/auth/user/users"))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readAllUsers-example", responseFields(
                      fieldWithPath("[]").description("An array of asserts"))
                      .andWithPrefix("[].", fieldpath)));
       
       userRepository.delete(user1.getId());
       userRepository.delete(user2.getId());
   }
   @Test
   public void createRoleExample() throws Exception {

       WormholeRole role = createRole();

       this.mockMvc.perform(post("/v1/auth/role/").contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(role)))
              .andExpect(status().isCreated())
              .andDo(document("AuthController-createRole-example", requestFields(
                      fieldWithPath("id").description("ID of FacilitySoftwareConfig, created by wormhole"),
                      fieldWithPath("roleName").description("roleName."),
                      fieldWithPath("privilegeNames").description("list of privilegeNames").type(List.class))));
       
   }
   @Test
   public void readOneRoleByIdExample() throws Exception {

       WormholeRole role = createRole();
       roleRepository.save(role);

       this.mockMvc.perform(get("/v1/auth/role/"+role.getId()+""))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readOneRoleById-example", responseFields(
                      fieldWithPath("id").description("ID of FacilitySoftwareConfig, created by wormhole"),
                      fieldWithPath("roleName").description("roleName."),
                      fieldWithPath("privilegeNames").description("list of privilegeNames").type(List.class))));
       
       roleRepository.delete(role.getId());
   }
   @Test
   public void readRoleByPageExample() throws Exception {

       WormholeRole role = createRole();
       roleRepository.save(role);

       this.mockMvc.perform(get("/v1/auth/role/page").content("{\"currentPage\":\"1\",\"pageSize\":\"5\"}")
               .param("currentPage", "1").param("pageSize", "5"))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readRoleByPage-example", requestFields(
                      fieldWithPath("currentPage").description("get datas for this page number."),
                      fieldWithPath("pageSize")
                            .description("The number of data displayed per page."))));
       
       roleRepository.delete(role.getId());
   }
   @Test
   public void readRoleNamesExample() throws Exception {

       WormholeRole role = createRole();
       roleRepository.save(role);
       
       FieldDescriptor[] fieldpath = new FieldDescriptor[] {
               fieldWithPath("id").description("ID of FacilitySoftwareConfig, created by wormhole"),
               fieldWithPath("roleName").description("roleName."),
               fieldWithPath("privilegeNames").description("list of privilegeNames").type(List.class)
               };
       
       this.mockMvc.perform(get("/v1/auth/roles"))
              .andExpect(status().isOk())
              .andDo(document("AuthController-readRoleNames-example", responseFields(
                      fieldWithPath("[]").description("An array of asserts"))
                      .andWithPrefix("[].", fieldpath)));
       
       roleRepository.delete(role.getId());
   }
   @Test
   public void deleteRoleExample() throws Exception {

       WormholeRole role = createRole();
       roleRepository.save(role);

       this.mockMvc.perform(delete("/v1/auth/role/"+role.getId()+"").content("{\"id\":\"" + role.getId() + "\"}"))
              .andExpect(status().isOk())
              .andDo(document("AuthController-deleteRole-example", requestFields(
                      fieldWithPath("id")
                      .description("ID of the AuthController, created by wormhole")
                )))
              .andReturn();
       
       roleRepository.delete(role.getId());
   }
   
   @Test
   public void updateRoleExample() throws Exception {

       WormholeRole role1 = createRole();
       roleRepository.save(role1);
       WormholeRole role2 = createRole();
       role2.setRoleName("rolename2");

       this.mockMvc.perform(put("/v1/auth/role").contentType(MediaType.APPLICATION_JSON_VALUE)
              .content(objectMapper.writeValueAsString(role2)))
              .andExpect(status().isOk())
              .andDo(document("AuthController-updateRole-example", requestFields(
                      fieldWithPath("id")
                      .description("ID of the Role, created by wormhole"),
                fieldWithPath("roleName").description("roleName."),
                fieldWithPath("privilegeNames").description("privilegeNames").type(String.class))))
              .andReturn();
       
       roleRepository.delete(role1.getId());
       roleRepository.delete(role2.getId());
   }
   
   AuthToken createToken(){
      AuthToken token = new AuthToken();
      token.setAccess_token("R$TYUIMJ");
      return token;
   }
   WormholeRole createRole() {
       List<String> privilegeNames = new ArrayList<String>();
       privilegeNames.add("privilegeName1");
       privilegeNames.add("privilegeName2");
       WormholeRole role = new WormholeRole();
       role.setId("roleid1");
       role.setPrivilegeNames(privilegeNames);
       role.setRoleName("roleName");
       return role;
   }
   WormholeUser createUser(){
       List<String> rolenames = new ArrayList<String>();
       rolenames.add("role1");
       rolenames.add("role2");
       List<String> userGroupIDs = new ArrayList<String>();
       userGroupIDs.add("userGroupIDs1");
       userGroupIDs.add("userGroupIDs2");
       
       WormholeUser user = new WormholeUser();
       user.setId("userid");
       user.setCreateTime(new Date());
       user.setEmailAddress("emailAddress");
       user.setGender(1);
       user.setLastPasswordResetDate(new Date().getTime());
       user.setMobile("mobile");
       user.setPassword("password");
       user.setRoleNames(rolenames);
       user.setStatus(0);
       user.setUserGroupIDs(userGroupIDs);
       user.setUserName("userName");

       return user;
   }

}
