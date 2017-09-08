## Vert.x demo for Mongo CRUD on Openshift

- Use oc cli for creating mongo pod.
```
$ oc process -f ocp/coolstore-catalog-mongodb-persistent.yaml -p CATALOG_DB_USERNAME=mongodb -p CATALOG_DB_PASSWORD=mongodb -p CATALOG_DATABASE=catalogdb | oc create -f -
```

- Add view role to default service account
```
$ oc policy add-role-to-user view -n $(oc project -q) -z default
```

- Create configmap, to externalise the application configuration
```
$ oc create configmap app-config --from-file=config/app-config.yml
```

- Deploy application using Fabric8 plugin
```
$ mvn clean fabric8:deploy -Popenshift
```

- Check the status of the `pod`
```
$ oc logs -f $(oc get pods | grep service | grep -v build | awk '{print $1}' )
```

- Test the application using `curl`
```
$ export CATALOG_URL=http://$(oc get route catalog-service | grep "catalog" | awk '{print $2}')
```
```
$ curl -XGET $CATALOG_URL/products
```
```
$ curl -XGET $CATALOG_URL/product/444435
```
```
$ curl -H "Content-Type: application/json" -X POST -d  '{"itemId" : "444441","name" : "Test","desc" : "test","price" : 106.0}' $CATALOG_URL/product
```
