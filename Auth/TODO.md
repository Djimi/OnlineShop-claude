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
