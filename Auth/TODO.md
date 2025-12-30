 .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                        in security config


---------------

Check this:

  web:
    resources:
      add-mappings: false


-------------------

Check this:

management:
  endpoints:
    web:
      exposure:
        include: health,env,configprops
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

----------------

We should add time check in the query, so we don't get expired sessions

----------

When we check for login we should check with case sensitivity, when we check for registration we should check withouth case sensitivity

---------

Add normaliztion for username and store normalized_uesrname


String normalized = Normalizer.normalize(request.getUsername().trim(), Normalizer.Form.NFKC)
.toLowerCase(Locale.ROOT);

------------
