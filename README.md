
# LUWRAIN

## How to compile?

At the moment LUWRAIN compilation requires installed Java SE 17 or later, Apache Maven and Apache Ant.
The last is needed for the compilation of apps, but the LUWRAIN core utilizes  Apache Maven only. 

In order to compile the LUWRAIN core without apps, just do ```mvn install```.
This will allow you to create your own apps.

If you would like to compile the entire code of LUWRAIN, including apps, please do the following (assuming you are running any Linux distro):

```bash
cd base/script
./lwr-ant-gen-all
./lwr-build
```

## Key publications 

* [Introducing LUWRAIN: Can GNU/Linux help us rethink accessibility solutions for the blind?](https://marigostra.ru/materials/LinuxJournal-2015-07.pdf) (Linux Journal, 2015)
*  [The framework for accessible applications: text-based case for blind people](https://dl.acm.org/doi/10.1145/2687233.2687234) (ACM Digital Library, 2014)

The both papers 
