# Catalina

![CI with Maven](https://github.com/Carlos-Augusto/cat/workflows/CI%20with%20Maven/badge.svg)

## General Description

![Commands](images/commandsWithDesc.png)

With this tool, you can interact with the core features of the _Ubirch Trust Platform_ (Cloud). 
You are able to send micro-certificates from different sources, files, user input, fixed strings. 
You can verify the micro-certificates after sending, which guaranties that your timestamp is now immutable and trust-enabled.

This is a tool that allows to use the _Ubirch Trust Service_. This tool is a command line.
Its features are:

* **create timestamps:** There are different kinds of timestamps you can create. You can create a timestamp based
on a file, on text, on user input, and random text.

All the above mentioned timestamps can be salted/added with a nonce.

* **register keys**

In order to send create a timestamp, a proper public key should be registered on the _Ubirch Trust Service_.
This option allows you to quickly register an existing key, or it allows you to generate a random key and 
identity id.

* **verify timestamps**

This feature verifies the generated timestamps. You can select the type of verification 
you would like to have. 

## Env Variables

For every one of these feature, you can set the stage at which the system will point to on the _Ubirch Platform_ [dev, demo, prod]
By default dev wil be used. 

To modify this, export the following variable. The following are the expected values: [dev, demo, prod]

```
export CAT_ENV=dev 
```

## Install

To install the latest version: 

_Download_ 

```shell script
curl -s https://github.com/Carlos-Augusto/cat/releases/download/${VERSION}/install.sh
```

_Options_

```shell script
install [-r] [-p] [-c CAT_HOME] [-e CAT_HOME]
-r -> will remove possible existing install
-c -> will remove possible existing install on custom place
-p -> will prompt if same install is found
-e -> will prompt if same install is found and will install on custom place
```

_Example_

Run script with best option for you.
```shell script
./install.sh -r
```

## Install from sources

Clone the project
```shell script
git clone git@github.com:Carlos-Augusto/cat.git 
```

Enter the cat folder or where you cloned it into
```shell script
cd cat 
```

Run the installation script. Use as argument the path for your catalina install.
```shell script
./build.sh PATH_FOR_INSTALL
```

For quick access, add your installation folder to your path.
```shell script
export PATH=$INSTALL_FOLDER:\$PATH"
```

Run to see the options
```shell script
./catalina.sh
```


