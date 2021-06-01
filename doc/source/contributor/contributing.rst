============================
So You Want to Contribute...
============================

For general information on contributing to OpenStack, please check out the
`contributor guide <https://docs.openstack.org/contributors/>`_ to get started.
It covers all the basics that are common to all OpenStack projects: the
accounts you need, the basics of interacting with our Gerrit review system,
how we communicate as a community, etc.

Below will cover the more project specific information you need to get started
with Monasca.

Communication
~~~~~~~~~~~~~
.. This would be a good place to put the channel you chat in as a project; when/
   where your meeting is, the tags you prepend to your ML threads, etc.

For communicating with Monasca Team, you can reach out to us on
*#openstack-monasca* IRC channel at OFTC.

We hold weekly `team meetings`_ in our IRC channel which is a good opportunity
to ask questions, propose new features or just get in touch with the team.

You can also send us an email to the mailing list
`openstack-discuss@lists.openstack.org`_. Please use *[Monasca]* tag for
easier thread filtering.

.. _team meetings: http://eavesdrop.openstack.org/#Monasca_Team_Meeting
.. _openstack-discuss@lists.openstack.org: http://lists.openstack.org/cgi-bin/mailman/listinfo/openstack-discuss

Contacting the Core Team
~~~~~~~~~~~~~~~~~~~~~~~~
.. This section should list the core team, their irc nicks, emails, timezones
   etc. If all this info is maintained elsewhere (i.e. a wiki), you can link to
   that instead of enumerating everyone here.

================== ========== =====
Name               IRC nick   Email
================== ========== =====
Martin Chacon Piza chaconpiza MartinDavid.ChaconPiza1@est.fujitsu.com
Witek Bedyk        witek      witold.bedyk@suse.com
Doug Szumski       dougsz     doug@stackhpc.com
Adrian Czarnecki   adriancz   adrian.czarnecki@ts.fujitsu.com
Joseph Davis       joadavis   joseph.davis@suse.com
================== ========== =====

New Feature Planning
~~~~~~~~~~~~~~~~~~~~
.. This section is for talking about the process to get a new feature in. Some
   projects use blueprints, some want specs, some want both! Some projects
   stick to a strict schedule when selecting what new features will be reviewed
   for a release.

Our process is meant to allow users, developers, and operators to express their
desires for new features using Storyboard stories. The workflow is very simple:

* If something is clearly broken, submit a `bug report`_ in Storyboard.
* If you want to change or add a feature, submit a `story`_ in Storyboard.
* Monasca core reviewers may request that you submit a `specification`_ to
  gerrit to elaborate on the feature request.
* Significant features require `release notes`_ to be included when the code is
  merged.

.. _story:

Stories
-------

New features can be proposed in `Storyboard
<https://storyboard.openstack.org/#!/project_group/59>`_ as new Story.

The initial story primarily needs to express the intent of the idea with
enough details that it can be evaluated for compatibility with the project
mission and whether or not the change requires a `specification`_. It is *not*
expected to contain all of the implementation details. If the feature is very
simple and well understood by the team, then describe it simply. The story is
then used to track all the related code reviews. Team members will
request more information as needed.

.. _specification:

Specifications
--------------

We use the `monasca-specs <https://github.com/openstack/monasca-specs>`_
repository for specification reviews. Specifications:

* Provide a review tool for collaborating on feedback and reviews for complex
  features.
* Collect team priorities.
* Serve as the basis for documenting the feature once implemented.
* Ensure that the overall impact on the system is considered.

.. _release notes:

Release Notes
-------------

The release notes for a patch should be included in the patch. If not, the
release notes should be in a follow-on review.

If any of the following applies to the patch, a release note is required:

* The deployer needs to take an action when upgrading
* A new feature is implemented
* Plugin API function was removed or changed
* Current behavior is changed
* A new config option is added that the deployer should consider changing from
  the default
* A security bug is fixed
* Change may break previous versions of the client library(ies)
* Requirement changes are introduced for important libraries like oslo, six
  requests, etc.
* Deprecation period starts or code is purged

A release note is suggested if a long-standing or important bug is fixed.
Otherwise, a release note is not required.

Task Tracking
~~~~~~~~~~~~~
.. This section is about where you track tasks- launchpad? storyboard? is there
   more than one launchpad project? what's the name of the project group in
   storyboard?

We track our tasks in Storyboard

https://storyboard.openstack.org/#!/project_group/monasca

If you're looking for some smaller, easier work item to pick up and get started
on, search for the *'low-hanging-fruit'* tag.

Kanban Board
------------

Progress on implementation of important stories in Ussuri release is tracked in
`Monasca Board on StoryBoard <https://storyboard.openstack.org/#!/board/190>`_.

.. _bug report:

Reporting a Bug
~~~~~~~~~~~~~~~
.. Pretty self explanatory section, link directly to where people should report
   bugs for your project.

You found an issue and want to make sure we are aware of it? You can `report
them on Storyboard <https://storyboard.openstack.org/#!/project_group/monasca>`_.

When filing a bug please remember to add the *bug* tag to the story. Please
provide information on what the problem is, how to replicate it, any
suggestions for fixing it, and a recommendation of the priority.

All open bugs can be found in this `Worklist
<https://storyboard.openstack.org/#!/worklist/213>`_.

Getting Your Patch Merged
~~~~~~~~~~~~~~~~~~~~~~~~~
.. This section should have info about what it takes to get something merged. Do
   you require one or two +2's before +W? Do some of your repos require unit
   test changes with all patches? etc.

All changes proposed to Monasca requires at least one ``Code-Review +2`` votes
from Monasca core reviewers before one of the core reviewers can approve
patch by giving ``Workflow +1`` vote.

Reviews Prioritisation
----------------------

Monasca project uses *Review-Priority* field in Gerrit to emphasize
prioritized code changes.

Every developer can propose the changes which should be prioritized
in `weekly team meeting <http://eavesdrop.openstack.org/#Monasca_Team_Meeting>`_
or in the mailing list. Any core reviewer,
preferably from a different company, can confirm such proposed change
by setting *Review-Priority* +1.

Prioritized changes can be listed in this
`dashboard <http://www.tinyurl.com/monasca>`_.

Project Team Lead Duties
~~~~~~~~~~~~~~~~~~~~~~~~
.. this section is where you can put PTL specific duties not already listed in
   the common PTL guide (linked below), or if you already have them written
   up elsewhere you can link to that doc here.

All common PTL duties are enumerated in the `PTL guide
<https://docs.openstack.org/project-team-guide/ptl.html>`_.
