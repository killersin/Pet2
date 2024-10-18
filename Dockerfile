FROM tomcat:9.0.46-jdk8

# Create a new user with a home directory and no password
RUN useradd -ms /bin/bash tomcatuser

# Set the new user as the owner of the Tomcat directory
RUN chown -R tomcatuser:tomcatuser /usr/local/tomcat

# Switch to the new user
USER tomcatuser

EXPOSE 8082
COPY target/petclinic.war /usr/local/tomcat/webapps/
