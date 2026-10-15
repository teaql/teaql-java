# Context-owned Secret Provider example

Run `bash examples/secret-provider/verify.sh`. The runtime-owned test binds a
symbolic reference to an owner-only local file at trusted startup and resolves
it through `UserContext` with a comment. Changing the file is visible at the
next resolution; the example runs twice and does not print secret content.
Vault and remote transport are outside this example.
