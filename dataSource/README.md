When the DB schema is updated, `build.gradle.kts` allows code to be generated with:
```
gradlew :dataSource:jooqCodegen
```

Then, if generated code uses excessive qualified names where imports would suffice:
```
(?<!import )com\.jeffeyalanwang\.dutchrailways\.backend\.dataSource\.generated(?:\.`public`(?:\.(?!Public)[a-zA-Z0-9]+(?:\.(?:records|references|paths))?)?)?\.
(?<![\._a-zA-Z0-9])([_a-zA-Z0-9]+(?=\.[_A-Z0-9]+[^_a-zA-Z0-9]))\.((?i)\1)(?![_a-zA-Z0-9])
```
Also check:
```
(?<![\._a-zA-Z0-9])(?:[_a-zA-Z0-9]*[a-z]+\.)([A-Z][_A-Z0-9]*)(?![_a-zA-Z0-9])
<([a-zA-Z0-9]+)> = configuration\.dsl\(\)\.selectFrom\(
```