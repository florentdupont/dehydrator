# Dehydrator (experimental)


Annotation Processor tool pour déshydrater des Bean Java.

Condition :

 * Toutes les objets du modèles doivent hériter d'une super-classe commune.




Exemple de contexte d'utilisation :
Les objets d'un




# Usage

il suffit de placer une annotation @Dehydrate sur l'objet à déshydrater.

Par exemple :

```
class Country extends AsbtractPersistable<Long> {

    String name;

    // getter + setter
}

@Entity
@Dehydrate
class User extends AbstractPersistable<Long> {

    String firstname;
    String lastname;

    Country country;

    // getter + setter
}

```

will create the following class :

```

class UserDto {
    Long id;
    String firstname;
    String lastname;
    Long coutryId;

    // getter + setter

}

```

# Options :


## of

réduit la génération des champs à aux champs spécifiés

si vide (par défaut), alors tous les champs sont inclus.

# excluded
Exclus certains champs de la génération du Dto.
Si vide (par défaut), alors aucun champs n'est exclus.

Si excluded est indiqué en meme temps que of, alors il est ignoré.

## suffix
par défaut : Dto
Spécifie le suffixe à utiliser pour l'objet déshydraté

## name
par défaut ""
Spécifie le nom de la classe déshydraté.
Dans ce cas, le champs suffix est ignoré.
Si vide (par défaut), reprend le meme nom que la classe source suffixée par le "suffix".

##




# Publication

Pour publier sur Maven Central :
http://datumedge.blogspot.fr/2012/05/publishing-from-github-to-maven-central.html